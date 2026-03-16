import re
import operator
import json

OPS = {"==": operator.eq, "!=": operator.ne, ">": operator.gt,
       ">=": operator.ge, "<": operator.lt, "<=": operator.le}

def evaluate_condition(condition: str, data: dict) -> bool:
    cond = condition.strip()
    if cond.upper() == "DEFAULT":
        return True

    # Handle && and ||
    if "&&" in cond:
        parts = cond.split("&&")
        return all(evaluate_condition(p.strip(), data) for p in parts)
    if "||" in cond:
        parts = cond.split("||")
        return any(evaluate_condition(p.strip(), data) for p in parts)

    # contains(field, "value")
    m = re.match(r'contains\((\w+),\s*["\']([^"\']+)["\']\)', cond)
    if m:
        field, val = m.group(1), m.group(2)
        return val in str(data.get(field, ""))

    # startsWith(field, "value")
    m = re.match(r'startsWith\((\w+),\s*["\']([^"\']+)["\']\)', cond)
    if m:
        field, val = m.group(1), m.group(2)
        return str(data.get(field, "")).startswith(val)

    # endsWith(field, "value")
    m = re.match(r'endsWith\((\w+),\s*["\']([^"\']+)["\']\)', cond)
    if m:
        field, val = m.group(1), m.group(2)
        return str(data.get(field, "")).endswith(val)

    # Comparison: field OP value
    for op_str in [">=", "<=", "!=", "==", ">", "<"]:
        if op_str in cond:
            left, right = cond.split(op_str, 1)
            left, right = left.strip(), right.strip()
            left_val = data.get(left, left)
            # Parse right side
            right = right.strip("'\"")
            try:
                right_val = float(right) if "." in right else int(right)
            except:
                right_val = right
            try:
                left_num = float(left_val) if isinstance(left_val, str) else left_val
                return OPS[op_str](left_num, right_val)
            except:
                return OPS[op_str](str(left_val), str(right_val))
    return False

def evaluate_rules(rules, data: dict):
    sorted_rules = sorted(rules, key=lambda r: r.priority)
    default_rule = None
    for rule in sorted_rules:
        if rule.condition.strip().upper() == "DEFAULT":
            default_rule = rule
            continue
        try:
            if evaluate_condition(rule.condition, data):
                return rule, f"Matched: {rule.condition}"
        except Exception as e:
            raise Exception(f"Rule eval error: {rule.condition} → {e}")
    if default_rule:
        return default_rule, "DEFAULT rule applied"
    return None, "No matching rule"
