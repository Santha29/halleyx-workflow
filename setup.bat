@echo off
title Halleyx Workflow - Auto Setup
color 0A

echo.
echo  ================================================
echo   Halleyx Workflow Engine - Auto Setup
echo  ================================================
echo.

echo [1/4] Installing Python dependencies...
pip install -r requirements.txt
echo Done!
echo.

echo [2/4] Installing VS Code Extensions...
call code --install-extension ms-python.python
call code --install-extension ms-python.pylance
call code --install-extension rangav.vscode-thunder-client
call code --install-extension qwtel.sqlite-viewer
call code --install-extension PKief.material-icon-theme
call code --install-extension usernamehw.errorlens
call code --install-extension eamodio.gitlens
call code --install-extension esbenp.prettier-vscode
echo Done!
echo.

echo [3/4] Opening project in VS Code...
call code .
echo Done!
echo.

echo [4/4] Starting server...
echo.
echo  ================================================
echo   Server starting at http://localhost:8080
echo   Press Ctrl+C to stop
echo  ================================================
echo.
uvicorn main:app --reload --port 8080

