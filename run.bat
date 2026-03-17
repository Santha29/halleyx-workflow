@echo off
echo.
echo  ================================
echo   Halleyx Workflow Engine
echo  ================================
echo.
echo Installing dependencies...
pip install fastapi uvicorn sqlalchemy pydantic -q
echo.
if exist halleyx_workflow.db del halleyx_workflow.db
echo Starting server...
echo Open: http://localhost:8080
echo.
python start.py
