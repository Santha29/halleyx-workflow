from sqlalchemy import Column, String, Integer, Boolean, Text, DateTime, ForeignKey, Enum
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import relationship
from datetime import datetime
import uuid, enum

Base = declarative_base()

def gen_uuid(): return str(uuid.uuid4())

class Workflow(Base):
    __tablename__ = "workflows"
    id = Column(String, primary_key=True, default=gen_uuid)
    name = Column(String, nullable=False)
    description = Column(Text)
    version = Column(Integer, default=1)
    is_active = Column(Boolean, default=True)
    input_schema = Column(Text)
    start_step_id = Column(String)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    steps = relationship("Step", back_populates="workflow", cascade="all, delete-orphan", order_by="Step.step_order")

class Step(Base):
    __tablename__ = "steps"
    id = Column(String, primary_key=True, default=gen_uuid)
    workflow_id = Column(String, ForeignKey("workflows.id"), nullable=False)
    name = Column(String, nullable=False)
    step_type = Column(String, nullable=False)  # TASK, APPROVAL, NOTIFICATION
    step_order = Column(Integer, default=1)
    metadata_json = Column(Text)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    workflow = relationship("Workflow", back_populates="steps")
    rules = relationship("Rule", back_populates="step", cascade="all, delete-orphan", order_by="Rule.priority")

class Rule(Base):
    __tablename__ = "rules"
    id = Column(String, primary_key=True, default=gen_uuid)
    step_id = Column(String, ForeignKey("steps.id"), nullable=False)
    condition = Column(Text, nullable=False)
    next_step_id = Column(String, nullable=True)
    priority = Column(Integer, nullable=False)
    description = Column(Text)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    step = relationship("Step", back_populates="rules")

class Execution(Base):
    __tablename__ = "executions"
    id = Column(String, primary_key=True, default=gen_uuid)
    workflow_id = Column(String, nullable=False)
    workflow_name = Column(String)
    workflow_version = Column(Integer)
    status = Column(String, default="PENDING")  # PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELED
    input_data = Column(Text)
    current_step_id = Column(String)
    current_step_name = Column(String)
    retries = Column(Integer, default=0)
    triggered_by = Column(String)
    error_message = Column(Text)
    started_at = Column(DateTime, default=datetime.utcnow)
    ended_at = Column(DateTime)
    logs = relationship("StepLog", back_populates="execution", cascade="all, delete-orphan", order_by="StepLog.executed_at")

class StepLog(Base):
    __tablename__ = "step_logs"
    id = Column(String, primary_key=True, default=gen_uuid)
    execution_id = Column(String, ForeignKey("executions.id"), nullable=False)
    step_id = Column(String)
    step_name = Column(String)
    step_type = Column(String)
    status = Column(String)
    rule_evaluated = Column(Text)
    next_step_id = Column(String)
    next_step_name = Column(String)
    message = Column(Text)
    approver_email = Column(String)
    duration_ms = Column(Integer)
    executed_at = Column(DateTime, default=datetime.utcnow)
    execution = relationship("Execution", back_populates="logs")
