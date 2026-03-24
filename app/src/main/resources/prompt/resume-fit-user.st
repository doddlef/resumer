# resume-job-fit-user.st

Company:
$company$

Position:
$position$

Job Description:
---JD START---
$jobDescription$
---JD END---

Known Key Requirements (if available):
$keyRequirements$

Candidate Resume Text:
---RESUME START---
$resumeText$
---RESUME END---

Task:
Evaluate how well the candidate’s resume aligns with the target role and generate high-impact, position-specific rewrite suggestions.

Focus on:
- True alignment between resume evidence and job requirements
- The most critical gaps affecting interview selection
- Practical improvements that increase screening success

Guidelines:
- Base all analysis strictly on the provided inputs
- Do NOT infer or invent missing experience
- Use the “Known Key Requirements” as a hint, not a strict constraint
- Treat the candidate as a student or early-career applicant
- Prioritize substance over wording-only improvements
- Focus on changes that materially improve recruiter or hiring manager decisions

Priorities:
1. Identify the most critical mismatches (gaps) against role expectations
2. Estimate overall fit for passing resume screening
3. Provide concrete, immediately applicable rewrite suggestions

Output Requirements:
- Return ONLY one valid JSON object
- Follow exactly the schema defined in the system prompt
- Keep content concise, specific, and actionable
- Avoid generic advice

