# Packaging Checklist

Use this after edits are complete.

1. Ensure `SKILL.md` frontmatter is valid YAML with only `name` and `description`.
2. Confirm all referenced files exist under the skill folder.
3. Run the packaging script from the repo root:

```bash
scripts/package_skill.py .claude/skills/java-sync
```

4. If validation fails, fix errors and re-run the command.
5. Share the generated `java-sync.skill` from the output directory (default is the current directory unless specified).
