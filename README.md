# MMAUG Insurance Claims Agent - Group B

**Customer:** MMAUG Insurance Ltd, Malta
**Project:** AI Agent to assist claims officers 

### Business Problem
Claims officers waste time organizing documents e.g photos, forms, police reports. We build AI agent to extract info, check missing orcontradictory data, retrieve policy via API, and produce summary with evidence references.

### RULES - AI Boundaries
Agent must not:
- Auto approve or reject claim
- Decide liability
- Authorize payment or modify policy
Human officer remains accountable.

### Git Workflow 
We are on GitHub Free Private Repo, so branch protection is NOT enforced by GitHub. We enforce by discipline.

- `main` = Production ready. Do not push directly.
- `develop` = Integration branch.
- `feature/<name>` = Your work. e.g., feature/frontend-login, feature/agent-extract

Workflow:
1. `git checkout develop`
2. `git pull origin develop`
3. `git checkout -b feature/your-task`
4. Work, commit
5. `git push origin feature/your-task`
6. Open Pull Request to `develop` and wait for 1 review
7. Lead merges develop to main

  Do not `git push origin main`

### Project Structure
/frontend - 
/backend -
/agent - AI Agent logic - Document extraction, Validation, Summary with sources
/infra - Terraform for Azure (Storage, App Service, Key Vault)
/docs - Proposal, Architecture diagram
/tests - Unit & integration tests
/data - Synthetic data

### MVP Demo Requirements


### DevOps Requirements 

Windows Java setup and current build/test evidence: [Java build baseline](docs/java-build-baseline.md).
- PR controls (this README rule)
- Automated tests (GitHub Actions)
- Dependency scanning (Dependabot)
- IaC (Terraform in /infra)
- Secrets via env variables, not hardcoded
