# resume-analysis-user.st

# Input Data
Analyze the following resume and generate a deep technical audit report. 
Use the "Technical Benchmark" only as inspiration — DO NOT force irrelevant patterns.

## Candidate Resume
---RESUME START---
$resumeText$
---RESUME END---

## Technical Benchmark (Reference Only)

Use these as examples of strong engineering signals. Adapt suggestions to the candidate’s domain 
(e.g., backend, data science, security, frontend, distributed systems). Do NOT blindly apply.

### High Concurrency & Performance
- Multi-level caching (e.g., Redis + local cache), handling cache penetration/breakdown
- Rate limiting using token bucket / leaky bucket / distributed control
- High QPS system design, latency reduction, load balancing

### Asynchronous Processing & Optimization
- Async orchestration (e.g., futures, event-driven design)
- Thread pool tuning and resource isolation
- Reducing response time and improving throughput

### Distributed Systems & Architecture
- Event-driven architecture (Kafka, RabbitMQ, etc.)
- Data consistency (eventual consistency, idempotency)
- API gateway, authentication, authorization (JWT, OAuth2, RBAC)
- Microservices and service communication

### Data Engineering & Data Science
- Feature engineering, model training, evaluation metrics
- Data pipelines (batch/stream processing)
- Model deployment, monitoring, and performance optimization

### Security Engineering
- Secure authentication and authorization design
- Common vulnerabilities (XSS, CSRF, SSRF, injection)
- Encryption, key management, secure communication (TLS)
- Threat modeling and risk mitigation

### System Design & Modeling
- Domain modeling and abstraction
- Design patterns (strategy, factory, observer, etc.)
- State management and workflow design
- Scalability and maintainability

### Reliability & Observability
- Monitoring, logging, tracing
- Fault tolerance, retries, circuit breakers
- Performance profiling and debugging
- Handling large-scale data

---

## Evaluation Dimensions

Evaluate the resume across:

1. Technical Depth  
   - Does it show understanding of underlying principles (e.g., concurrency, indexing, system design)?

2. Business Impact  
   - Does it clearly explain what problem was solved and why it matters?

3. Measurable Results  
   - Are there metrics (latency, throughput, accuracy, scale)?

4. Expression Quality  
   - Are bullet points action-driven, clear, and concise?

5. Contribution Clarity  
   - Does it clearly show what the candidate personally did?

6. Relevance to Target Roles  
   - Is the resume aligned with realistic job expectations?

---

## Instructions

- Provide a deep, critical, and practical analysis
- Do NOT fabricate experiences
- You MAY suggest reasonable metrics if missing (clearly as suggestions)
- Focus especially on:
  - Project experience rewriting
  - Making contributions explicit
  - Increasing competitiveness

- All improvements MUST include:
  - Original sentence vs Improved sentence

---

## Output Requirement

Return STRICT JSON only.

Structure:

{
  "score": integer (0-100),
  "summary": "one sentence summary",
  "strengths": ["string"],
  "suggestions": [
    {
      "category": "content/format/skills/project",
      "priority": "high/medium/low",
      "issue": "description of the problem",
      "recommendation": "must include original vs improved sentence"
    }
  ]
}
