import os, sys, subprocess, webbrowser, time

print("=" * 50)
print("  Halleyx Workflow Engine - Starting...")
print("=" * 50)

# Install dependencies
print("\n📦 Installing dependencies...")
subprocess.run([sys.executable, "-m", "pip", "install", 
    "fastapi", "uvicorn", "sqlalchemy", "pydantic", "--quiet"])
print("✅ Dependencies ready!")

# Delete old DB for fresh start
if os.path.exists("halleyx_workflow.db"):
    os.remove("halleyx_workflow.db")
    print("🗑️  Old database cleared!")

# Open browser after 3 seconds
def open_browser():
    time.sleep(4)
    webbrowser.open("http://localhost:8080")

import threading
threading.Thread(target=open_browser, daemon=True).start()

print("\n🚀 Server starting at http://localhost:8080")
print("📖 API Docs at http://localhost:8080/docs")
print("\n   Press Ctrl+C to stop\n")
print("=" * 50)

# Run server
os.system(f"{sys.executable} -m uvicorn main:app --reload --port 8080")
