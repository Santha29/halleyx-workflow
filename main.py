from fastapi import FastAPI, HTTPException
from fastapi.responses import HTMLResponse
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session
from pydantic import BaseModel
from typing import Optional
from datetime import datetime
from models import Base, Workflow, Step, Rule, Execution, StepLog, gen_uuid
from rule_engine import evaluate_rules
import json, time, os

# ── DB ──
engine = create_engine("sqlite:///./halleyx_workflow.db", connect_args={"check_same_thread": False})
Base.metadata.create_all(bind=engine)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

app = FastAPI(title="Halleyx Workflow Engine", version="1.0.0")

def get_db():
    db = SessionLocal()
    try: yield db
    finally: db.close()

# ── Schemas ──
class WorkflowReq(BaseModel):
    name: str
    description: Optional[str] = None
    input_schema: Optional[str] = None
    start_step_id: Optional[str] = None
    is_active: Optional[bool] = True

class StepReq(BaseModel):
    name: str
    step_type: str
    step_order: Optional[int] = None
    metadata_json: Optional[str] = None

class RuleReq(BaseModel):
    condition: str
    next_step_id: Optional[str] = None
    priority: int
    description: Optional[str] = None

class ExecutionReq(BaseModel):
    input_data: Optional[dict] = {}
    triggered_by: Optional[str] = "system"

def ok(data, msg="Success"):
    return {"success": True, "message": msg, "data": data, "timestamp": datetime.utcnow().isoformat()}

def err(msg, code=400):
    raise HTTPException(status_code=code, detail={"success": False, "message": msg})

# ── Serializers ──
def rule_to_dict(r):
    return {"id": r.id, "step_id": r.step_id, "condition": r.condition,
            "next_step_id": r.next_step_id, "priority": r.priority,
            "description": r.description,
            "created_at": r.created_at.isoformat() if r.created_at else None,
            "updated_at": r.updated_at.isoformat() if r.updated_at else None}

def step_to_dict(s, include_rules=False):
    d = {"id": s.id, "workflow_id": s.workflow_id, "name": s.name,
         "step_type": s.step_type, "step_order": s.step_order,
         "metadata": s.metadata_json, "rule_count": len(s.rules),
         "created_at": s.created_at.isoformat() if s.created_at else None,
         "updated_at": s.updated_at.isoformat() if s.updated_at else None}
    if include_rules:
        d["rules"] = [rule_to_dict(r) for r in sorted(s.rules, key=lambda x: x.priority)]
    return d

def wf_to_dict(wf, include_steps=False):
    d = {"id": wf.id, "name": wf.name, "description": wf.description,
         "version": wf.version, "is_active": wf.is_active,
         "input_schema": wf.input_schema, "start_step_id": wf.start_step_id,
         "step_count": len(wf.steps),
         "created_at": wf.created_at.isoformat() if wf.created_at else None,
         "updated_at": wf.updated_at.isoformat() if wf.updated_at else None}
    if include_steps:
        d["steps"] = [step_to_dict(s, include_rules=True) for s in sorted(wf.steps, key=lambda x: x.step_order or 0)]
    return d

def log_to_dict(l):
    return {"id": l.id, "step_id": l.step_id, "step_name": l.step_name,
            "step_type": l.step_type, "status": l.status,
            "rule_evaluated": l.rule_evaluated,
            "next_step_id": l.next_step_id, "next_step_name": l.next_step_name,
            "message": l.message, "approver_email": l.approver_email,
            "duration_ms": l.duration_ms,
            "executed_at": l.executed_at.isoformat() if l.executed_at else None}

def exec_to_dict(e, db: Session, include_logs=False):
    dur = None
    if e.started_at and e.ended_at:
        dur = int((e.ended_at - e.started_at).total_seconds())
    # Always fetch logs from DB fresh to avoid lazy load issues
    logs = db.query(StepLog).filter(StepLog.execution_id == e.id).order_by(StepLog.executed_at).all()
    d = {"id": e.id, "workflow_id": e.workflow_id, "workflow_name": e.workflow_name,
         "workflow_version": e.workflow_version, "status": e.status,
         "input_data": json.loads(e.input_data) if e.input_data else {},
         "current_step_id": e.current_step_id, "current_step_name": e.current_step_name,
         "retries": e.retries, "triggered_by": e.triggered_by,
         "error_message": e.error_message, "duration_seconds": dur,
         "step_count": len(logs),
         "started_at": e.started_at.isoformat() if e.started_at else None,
         "ended_at": e.ended_at.isoformat() if e.ended_at else None}
    if include_logs:
        d["logs"] = [log_to_dict(l) for l in logs]
    else:
        d["logs"] = []
    return d

# ── WORKFLOW CRUD ──
@app.post("/api/v1/workflows")
def create_workflow(req: WorkflowReq):
    db = next(get_db())
    wf = Workflow(id=gen_uuid(), name=req.name, description=req.description,
                  input_schema=req.input_schema, is_active=req.is_active,
                  start_step_id=req.start_step_id, version=1)
    db.add(wf); db.commit(); db.refresh(wf)
    return ok(wf_to_dict(wf), "Workflow created")

@app.get("/api/v1/workflows")
def list_workflows(name: Optional[str] = None, isActive: Optional[bool] = None,
                   page: int = 0, size: int = 10):
    db = next(get_db())
    q = db.query(Workflow)
    if name: q = q.filter(Workflow.name.ilike(f"%{name}%"))
    if isActive is not None: q = q.filter(Workflow.is_active == isActive)
    total = q.count()
    items = q.order_by(Workflow.created_at.desc()).offset(page * size).limit(size).all()
    return ok({"content": [wf_to_dict(w) for w in items],
               "totalElements": total, "totalPages": max(1,(total + size - 1) // size), "page": page})

@app.get("/api/v1/workflows/{wf_id}")
def get_workflow(wf_id: str):
    db = next(get_db())
    wf = db.query(Workflow).filter(Workflow.id == wf_id).first()
    if not wf: err("Workflow not found", 404)
    return ok(wf_to_dict(wf, include_steps=True))

@app.put("/api/v1/workflows/{wf_id}")
def update_workflow(wf_id: str, req: WorkflowReq):
    db = next(get_db())
    wf = db.query(Workflow).filter(Workflow.id == wf_id).first()
    if not wf: err("Workflow not found", 404)
    wf.name = req.name; wf.description = req.description
    wf.input_schema = req.input_schema
    if req.is_active is not None: wf.is_active = req.is_active
    wf.version += 1; wf.updated_at = datetime.utcnow()
    db.commit(); db.refresh(wf)
    return ok(wf_to_dict(wf, include_steps=True), "Workflow updated")

@app.delete("/api/v1/workflows/{wf_id}")
def delete_workflow(wf_id: str):
    db = next(get_db())
    wf = db.query(Workflow).filter(Workflow.id == wf_id).first()
    if not wf: err("Workflow not found", 404)
    db.delete(wf); db.commit()
    return ok(None, "Workflow deleted")

# ── STEPS ──
@app.post("/api/v1/workflows/{wf_id}/steps")
def create_step(wf_id: str, req: StepReq):
    db = next(get_db())
    wf = db.query(Workflow).filter(Workflow.id == wf_id).first()
    if not wf: err("Workflow not found", 404)
    existing = db.query(Step).filter(Step.workflow_id == wf_id).all()
    order = req.step_order or (max([s.step_order or 0 for s in existing], default=0) + 1)
    s = Step(id=gen_uuid(), workflow_id=wf_id, name=req.name,
             step_type=req.step_type.upper(), step_order=order, metadata_json=req.metadata_json)
    db.add(s); db.commit(); db.refresh(s)
    return ok(step_to_dict(s, include_rules=True), "Step created")

@app.get("/api/v1/workflows/{wf_id}/steps")
def list_steps(wf_id: str):
    db = next(get_db())
    steps = db.query(Step).filter(Step.workflow_id == wf_id).order_by(Step.step_order).all()
    return ok([step_to_dict(s, include_rules=True) for s in steps])

@app.get("/api/v1/steps/{step_id}")
def get_step(step_id: str):
    db = next(get_db())
    s = db.query(Step).filter(Step.id == step_id).first()
    if not s: err("Step not found", 404)
    return ok(step_to_dict(s, include_rules=True))

@app.put("/api/v1/steps/{step_id}")
def update_step(step_id: str, req: StepReq):
    db = next(get_db())
    s = db.query(Step).filter(Step.id == step_id).first()
    if not s: err("Step not found", 404)
    s.name = req.name; s.step_type = req.step_type.upper()
    if req.step_order: s.step_order = req.step_order
    if req.metadata_json is not None: s.metadata_json = req.metadata_json
    s.updated_at = datetime.utcnow()
    db.commit(); db.refresh(s)
    return ok(step_to_dict(s, include_rules=True), "Step updated")

@app.delete("/api/v1/steps/{step_id}")
def delete_step(step_id: str):
    db = next(get_db())
    s = db.query(Step).filter(Step.id == step_id).first()
    if not s: err("Step not found", 404)
    db.delete(s); db.commit()
    return ok(None, "Step deleted")

# ── RULES ──
@app.post("/api/v1/steps/{step_id}/rules")
def create_rule(step_id: str, req: RuleReq):
    db = next(get_db())
    s = db.query(Step).filter(Step.id == step_id).first()
    if not s: err("Step not found", 404)
    r = Rule(id=gen_uuid(), step_id=step_id, condition=req.condition,
             next_step_id=req.next_step_id, priority=req.priority, description=req.description)
    db.add(r); db.commit(); db.refresh(r)
    return ok(rule_to_dict(r), "Rule created")

@app.get("/api/v1/steps/{step_id}/rules")
def list_rules(step_id: str):
    db = next(get_db())
    rules = db.query(Rule).filter(Rule.step_id == step_id).order_by(Rule.priority).all()
    return ok([rule_to_dict(r) for r in rules])

@app.put("/api/v1/rules/{rule_id}")
def update_rule(rule_id: str, req: RuleReq):
    db = next(get_db())
    r = db.query(Rule).filter(Rule.id == rule_id).first()
    if not r: err("Rule not found", 404)
    r.condition = req.condition; r.next_step_id = req.next_step_id
    r.priority = req.priority; r.description = req.description
    r.updated_at = datetime.utcnow()
    db.commit(); db.refresh(r)
    return ok(rule_to_dict(r), "Rule updated")

@app.delete("/api/v1/rules/{rule_id}")
def delete_rule(rule_id: str):
    db = next(get_db())
    r = db.query(Rule).filter(Rule.id == rule_id).first()
    if not r: err("Rule not found", 404)
    db.delete(r); db.commit()
    return ok(None, "Rule deleted")

# ── EXECUTION ENGINE ──
MAX_ITERATIONS = 50

def run_workflow(execution: Execution, wf: Workflow, input_data: dict, db: Session):
    steps = db.query(Step).filter(Step.workflow_id == wf.id).order_by(Step.step_order).all()
    if not steps:
        raise Exception("Workflow has no steps defined")
    step_map = {s.id: s for s in steps}
    cur = step_map.get(wf.start_step_id, steps[0]) if wf.start_step_id else steps[0]
    iterations = 0

    while cur:
        if iterations >= MAX_ITERATIONS:
            raise Exception(f"Max loop iterations ({MAX_ITERATIONS}) reached — possible infinite loop!")
        iterations += 1
        t_start = time.time()

        execution.current_step_id = cur.id
        execution.current_step_name = cur.name
        db.commit()

        rules = db.query(Rule).filter(Rule.step_id == cur.id).order_by(Rule.priority).all()
        matched_rule, rule_msg = evaluate_rules(rules, input_data)

        meta = {}
        if cur.metadata_json:
            try: meta = json.loads(cur.metadata_json)
            except: pass

        approver = None
        step_msg = ""
        if cur.step_type == "APPROVAL":
            approver = meta.get("assignee_email")
            step_msg = f"Approval processed. Approver: {approver or 'N/A'}"
        elif cur.step_type == "NOTIFICATION":
            ch = meta.get("notification_channel", "email")
            tmpl = meta.get("template", "")
            step_msg = f"Notification sent via {ch}" + (f". Template: {tmpl}" if tmpl else "")
        elif cur.step_type == "TASK":
            instr = meta.get("instructions", "")
            step_msg = f"Task executed. {instr}".strip()

        next_step_id = matched_rule.next_step_id if matched_rule else None
        next_step_name = None
        if next_step_id and next_step_id in step_map:
            next_step_name = step_map[next_step_id].name

        duration_ms = int((time.time() - t_start) * 1000)
        log = StepLog(
            id=gen_uuid(), execution_id=execution.id,
            step_id=cur.id, step_name=cur.name, step_type=cur.step_type,
            status="COMPLETED",
            rule_evaluated=matched_rule.condition if matched_rule else "No rule matched",
            next_step_id=next_step_id, next_step_name=next_step_name,
            message=f"{step_msg} | {rule_msg}",
            approver_email=approver, duration_ms=duration_ms
        )
        db.add(log); db.commit()
        cur = step_map.get(next_step_id) if next_step_id else None

    execution.status = "COMPLETED"
    execution.current_step_id = None
    execution.current_step_name = None
    execution.ended_at = datetime.utcnow()
    db.commit()

@app.post("/api/v1/workflows/{wf_id}/execute")
def execute_workflow(wf_id: str, req: ExecutionReq = None):
    if req is None: req = ExecutionReq()
    db = next(get_db())
    wf = db.query(Workflow).filter(Workflow.id == wf_id).first()
    if not wf: err("Workflow not found", 404)
    if not wf.is_active: err("Workflow is not active")

    if wf.input_schema:
        try:
            schema = json.loads(wf.input_schema)
            for field in schema.get("required", []):
                if field not in (req.input_data or {}):
                    err(f"Missing required field: '{field}'")
        except HTTPException: raise
        except: pass

    ex = Execution(id=gen_uuid(), workflow_id=wf.id, workflow_name=wf.name,
                   workflow_version=wf.version, status="IN_PROGRESS",
                   input_data=json.dumps(req.input_data or {}),
                   triggered_by=req.triggered_by or "system", retries=0)
    db.add(ex); db.commit(); db.refresh(ex)

    try:
        run_workflow(ex, wf, req.input_data or {}, db)
    except Exception as e:
        ex.status = "FAILED"
        ex.error_message = str(e)
        ex.ended_at = datetime.utcnow()
        db.commit()

    db.refresh(ex)
    return ok(exec_to_dict(ex, db, include_logs=True), "Workflow executed")

@app.get("/api/v1/executions")
def list_executions(workflowId: Optional[str] = None, status: Optional[str] = None,
                    page: int = 0, size: int = 10):
    db = next(get_db())
    q = db.query(Execution)
    if workflowId: q = q.filter(Execution.workflow_id == workflowId)
    if status: q = q.filter(Execution.status == status.upper())
    total = q.count()
    items = q.order_by(Execution.started_at.desc()).offset(page * size).limit(size).all()
    return ok({"content": [exec_to_dict(e, db, include_logs=True) for e in items],
               "totalElements": total, "totalPages": max(1,(total + size - 1) // size), "page": page})

@app.get("/api/v1/executions/{exec_id}")
def get_execution(exec_id: str):
    db = next(get_db())
    ex = db.query(Execution).filter(Execution.id == exec_id).first()
    if not ex: err("Execution not found", 404)
    return ok(exec_to_dict(ex, db, include_logs=True))

@app.post("/api/v1/executions/{exec_id}/cancel")
def cancel_execution(exec_id: str):
    db = next(get_db())
    ex = db.query(Execution).filter(Execution.id == exec_id).first()
    if not ex: err("Execution not found", 404)
    if ex.status not in ["IN_PROGRESS", "PENDING"]:
        err(f"Cannot cancel execution with status: {ex.status}")
    ex.status = "CANCELED"; ex.ended_at = datetime.utcnow()
    ex.error_message = "Cancelled by user"; db.commit()
    return ok(exec_to_dict(ex, db, include_logs=True), "Cancelled")

@app.post("/api/v1/executions/{exec_id}/retry")
def retry_execution(exec_id: str):
    db = next(get_db())
    ex = db.query(Execution).filter(Execution.id == exec_id).first()
    if not ex: err("Execution not found", 404)
    if ex.status != "FAILED": err("Only FAILED executions can be retried")
    wf = db.query(Workflow).filter(Workflow.id == ex.workflow_id).first()
    if not wf: err("Workflow not found", 404)
    ex.status = "IN_PROGRESS"; ex.retries += 1
    ex.ended_at = None; ex.error_message = None; db.commit()
    try:
        input_data = json.loads(ex.input_data) if ex.input_data else {}
        run_workflow(ex, wf, input_data, db)
    except Exception as e:
        ex.status = "FAILED"; ex.error_message = str(e)
        ex.ended_at = datetime.utcnow(); db.commit()
    db.refresh(ex)
    return ok(exec_to_dict(ex, db, include_logs=True), "Retried")

# ── FRONTEND ──
@app.get("/", response_class=HTMLResponse)
def serve_ui():
    paths = [
        os.path.join(os.path.dirname(__file__), "src", "main", "resources", "static", "index.html"),
        os.path.join(os.path.dirname(__file__), "static", "index.html"),
    ]
    for p in paths:
        if os.path.exists(p):
            with open(p, encoding="utf-8") as f:
                return f.read()
    return "<h1>Halleyx Workflow Engine</h1><p><a href='/docs'>API Docs</a></p>"

# ── SEED DATA ──
def seed():
    db = SessionLocal()
    if db.query(Workflow).count() > 0:
        db.close(); return
    print("🌱 Seeding sample workflows...")

    wf1 = Workflow(id=gen_uuid(), name="Expense Approval",
                   description="Multi-step expense approval with finance and CEO sign-off",
                   version=1, is_active=True,
                   input_schema='{"required":["amount","country","priority"]}')
    db.add(wf1); db.flush()

    s1 = Step(id=gen_uuid(), workflow_id=wf1.id, name="Manager Approval", step_type="APPROVAL", step_order=1,
              metadata_json='{"assignee_email":"manager@company.com","instructions":"Review expense claim"}')
    s2 = Step(id=gen_uuid(), workflow_id=wf1.id, name="Finance Notification", step_type="NOTIFICATION", step_order=2,
              metadata_json='{"notification_channel":"email","template":"finance-review"}')
    s3 = Step(id=gen_uuid(), workflow_id=wf1.id, name="CEO Approval", step_type="APPROVAL", step_order=3,
              metadata_json='{"assignee_email":"ceo@company.com","instructions":"Final approval for high-value expenses"}')
    s4 = Step(id=gen_uuid(), workflow_id=wf1.id, name="Task Completion", step_type="TASK", step_order=4,
              metadata_json='{"instructions":"Mark expense approved and notify requester"}')
    s5 = Step(id=gen_uuid(), workflow_id=wf1.id, name="Task Rejection", step_type="TASK", step_order=5,
              metadata_json='{"instructions":"Mark expense rejected and notify requester"}')
    db.add_all([s1,s2,s3,s4,s5]); db.flush()
    wf1.start_step_id = s1.id

    db.add_all([
        Rule(id=gen_uuid(), step_id=s1.id, priority=1, condition="amount > 100 && country == 'US' && priority == 'High'", next_step_id=s2.id, description="High value US expense → Finance"),
        Rule(id=gen_uuid(), step_id=s1.id, priority=2, condition="amount <= 100", next_step_id=s4.id, description="Low value → Direct approval"),
        Rule(id=gen_uuid(), step_id=s1.id, priority=3, condition="priority == 'Low' && country != 'US'", next_step_id=s5.id, description="Low priority non-US → Reject"),
        Rule(id=gen_uuid(), step_id=s1.id, priority=4, condition="DEFAULT", next_step_id=s5.id, description="Default → Reject"),
        Rule(id=gen_uuid(), step_id=s2.id, priority=1, condition="amount > 10000", next_step_id=s3.id, description="Very high value → CEO approval"),
        Rule(id=gen_uuid(), step_id=s2.id, priority=2, condition="DEFAULT", next_step_id=s4.id, description="Default → Approve"),
        Rule(id=gen_uuid(), step_id=s3.id, priority=1, condition="DEFAULT", next_step_id=s4.id, description="CEO approved"),
        Rule(id=gen_uuid(), step_id=s4.id, priority=1, condition="DEFAULT", next_step_id=None, description="Workflow ends"),
        Rule(id=gen_uuid(), step_id=s5.id, priority=1, condition="DEFAULT", next_step_id=None, description="Workflow ends"),
    ])

    wf2 = Workflow(id=gen_uuid(), name="Employee Onboarding",
                   description="Automated employee onboarding flow",
                   version=1, is_active=True,
                   input_schema='{"required":["department","role"]}')
    db.add(wf2); db.flush()
    t1 = Step(id=gen_uuid(), workflow_id=wf2.id, name="Account Setup", step_type="TASK", step_order=1,
              metadata_json='{"instructions":"Create accounts and assign system access"}')
    t2 = Step(id=gen_uuid(), workflow_id=wf2.id, name="HR Approval", step_type="APPROVAL", step_order=2,
              metadata_json='{"assignee_email":"hr@company.com","instructions":"Approve onboarding"}')
    t3 = Step(id=gen_uuid(), workflow_id=wf2.id, name="Welcome Notification", step_type="NOTIFICATION", step_order=3,
              metadata_json='{"notification_channel":"slack","template":"welcome-employee"}')
    db.add_all([t1,t2,t3]); db.flush()
    wf2.start_step_id = t1.id
    db.add_all([
        Rule(id=gen_uuid(), step_id=t1.id, priority=1, condition="department == 'Engineering'", next_step_id=t2.id, description="Engineering → HR Approval"),
        Rule(id=gen_uuid(), step_id=t1.id, priority=2, condition="DEFAULT", next_step_id=t3.id, description="Others → Welcome"),
        Rule(id=gen_uuid(), step_id=t2.id, priority=1, condition="DEFAULT", next_step_id=t3.id, description="After HR → Welcome"),
        Rule(id=gen_uuid(), step_id=t3.id, priority=1, condition="DEFAULT", next_step_id=None, description="Onboarding complete"),
    ])

    # Seed sample executions so dashboard shows data immediately
    for wf, inp in [(wf1, {"amount": 500, "country": "US", "priority": "High"}),
                    (wf1, {"amount": 50, "country": "US", "priority": "Low"}),
                    (wf2, {"department": "Engineering", "role": "Backend"}),
                    (wf2, {"department": "Marketing", "role": "Manager"})]:
        db.flush()
        ex = Execution(id=gen_uuid(), workflow_id=wf.id, workflow_name=wf.name,
                       workflow_version=wf.version, status="IN_PROGRESS",
                       input_data=json.dumps(inp), triggered_by="seed@halleyx.com", retries=0)
        db.add(ex); db.flush()
        try:
            run_workflow(ex, wf, inp, db)
        except Exception as e:
            ex.status = "FAILED"; ex.error_message = str(e)
            ex.ended_at = datetime.utcnow(); db.commit()

    db.commit(); db.close()
    print("✅ Seeded 2 workflows + 4 sample executions!")

seed()
