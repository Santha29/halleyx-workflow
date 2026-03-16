@echo off
echo Installing dependencies...
pip install -r requirements.txt
echo.
echo Starting Halleyx Workflow Engine...
echo Open browser: http://localhost:8080
echo Swagger API:  http://localhost:8080/docs
echo.
uvicorn main:app --reload --port 8080
