# FastAPI application entry point — exposes messaging CRUD, health, and info endpoints.
import os
from typing import List

from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(
    title="Messaging API",
    version=os.getenv("APP_VERSION", "0.0.0"),
)


class MessageIn(BaseModel):
    id: str
    nickname: str
    message: str


class MessageOut(MessageIn):
    pass


class InfoOut(BaseModel):
    version: str
    environment: str
    build_sha: str


messages: List[MessageOut] = []


@app.post("/messages", response_model=MessageOut, status_code=201)
def create_message(body: MessageIn):
    """Store a new message and return it."""
    msg = MessageOut(**body.model_dump())
    messages.append(msg)
    return msg


@app.get("/messages", response_model=List[MessageOut])
def list_messages():
    """Return all stored messages."""
    return messages


@app.get("/info", response_model=InfoOut)
def info():
    """Return runtime metadata: version, environment, and build SHA."""
    return InfoOut(
        version=os.getenv("APP_VERSION", "unknown"),
        environment=os.getenv("ENVIRONMENT", "unknown"),
        build_sha=os.getenv("BUILD_SHA", "unknown"),
    )


@app.get("/health")
def health():
    """Liveness probe endpoint — returns 200 when the service is running."""
    return {"status": "ok"}

@app.get("/test")
def test():
    return {"status": "test"}

@app.get("/test2")
def test2():
    return {"status": "test2"}

@app.get("/test3")
def test3():
    return {"status": "test3"}

@app.get("/test4")
def test4():
    return {"status": "test4"}
