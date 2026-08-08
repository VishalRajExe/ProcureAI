# context-loader.md

Part of the Runtime Layer (Phase 4). Each runtime component performs exactly one responsibility.

## Responsibility
Loads only the memory and graph context relevant to the affected nodes returned by the Graph Retriever.

## Example
For a prompt like "Add Dark Mode":
- Affected Nodes: Navbar, Theme Context, Settings, Theme Provider
- Loads only: frontend.md, architecture.md, Theme Context
