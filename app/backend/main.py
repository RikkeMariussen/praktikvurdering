import os
import re
from pathlib import Path

from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from openai import OpenAI
from pydantic import BaseModel

load_dotenv()

app = FastAPI()

ALLOWED_ORIGINS = [
    "http://localhost:5173",
    *os.getenv("CORS_ORIGINS", "").split(","),
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=[o for o in ALLOWED_ORIGINS if o],
    allow_methods=["*"],
    allow_headers=["*"],
)

BASE = Path(__file__).parent.parent.parent  # praktikvurdering/
DATA_DIR = BASE / "data"
PROMPTS_DIR = BASE / "prompts"
OUTPUT_DIR = BASE / "output"

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))
MODEL = os.getenv("OPENAI_MODEL", "gpt-4o")

SAFE_FILENAME = re.compile(r"^[\w\-]+\.md$")


def safe_filename(filename: str) -> None:
    if not SAFE_FILENAME.match(filename):
        raise HTTPException(status_code=400, detail="Ugyldigt filnavn")


@app.get("/reports")
def list_reports():
    files = sorted(f.name for f in DATA_DIR.glob("*.md"))
    return {"reports": files}


@app.get("/output")
def list_outputs():
    OUTPUT_DIR.mkdir(exist_ok=True)
    files = sorted(f.name for f in OUTPUT_DIR.glob("*.md"))
    return {"outputs": files}


@app.get("/output/{filename}")
def get_output(filename: str):
    safe_filename(filename)
    path = OUTPUT_DIR / filename
    if not path.exists():
        raise HTTPException(status_code=404, detail="Fil ikke fundet")
    return {"content": path.read_text(encoding="utf-8")}


class EvaluateRequest(BaseModel):
    filename: str
    notes: str = ""


@app.post("/evaluate")
def evaluate(req: EvaluateRequest):
    safe_filename(req.filename)

    report_path = DATA_DIR / req.filename
    if not report_path.exists():
        raise HTTPException(status_code=404, detail="Rapport ikke fundet")

    system_prompt = (PROMPTS_DIR / "system-prompt.md").read_text(encoding="utf-8")
    report_content = report_path.read_text(encoding="utf-8")

    stem = report_path.stem
    output_filename = f"{stem}-vurdering.md"

    user_message = "Vurdér følgende praktikrapport ud fra rubricen.\n\n"
    if req.notes.strip():
        user_message += f"**Noter til eksaminator:**\n{req.notes.strip()}\n\n"
    user_message += f"## Rapport\n\n{report_content}\n\n"
    user_message += f"Gem din vurdering i filen `output/{output_filename}`."

    response = client.chat.completions.create(
        model=MODEL,
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_message},
        ],
    )

    result = response.choices[0].message.content

    OUTPUT_DIR.mkdir(exist_ok=True)
    (OUTPUT_DIR / output_filename).write_text(result, encoding="utf-8")

    return {"result": result, "output_file": output_filename}
