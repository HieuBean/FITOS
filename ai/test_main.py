from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_chat_without_token():
    response = client.post("/llm/chat", json={"prompt": "Hello"})
    assert response.status_code in (401, 403)
    assert response.json() == {"detail": "Not authenticated"}

def test_chat_with_invalid_token():
    headers = {"Authorization": "Bearer invalid-token"}
    response = client.post("/llm/chat", headers=headers, json={"prompt": "Hello"})
    assert response.status_code == 403
    assert response.json() == {"detail": "Invalid token"}

def test_chat_with_valid_token():
    headers = {"Authorization": "Bearer fitos-ai-token"}
    response = client.post("/llm/chat", headers=headers, json={"prompt": "Explain FITOS"})
    assert response.status_code == 200
    assert "Received prompt: Explain FITOS" in response.json()["answer"]
