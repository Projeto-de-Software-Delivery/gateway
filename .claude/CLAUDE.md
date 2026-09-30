# Fluxo de trabalho com Jira

O Jira já tem automação nativa (dev panel/GitHub integration) que transiciona
a issue sozinha: criar a branch move para "Em andamento", abrir PR move para
"Em análise", e o merge do PR move para "Concluído". Esse é o único mecanismo
de transição — não fazer transição manual por MCP, exceto para corrigir um
caso que a automação claramente não cobriu.

- Toda issue trabalhada precisa de uma branch **própria e dedicada**, nomeada
  `KAN-<numero>-<slug>` (o slug sugerido pela própria issue no Jira).
- Nunca commitar ou dar push direto em `main`. Todo trabalho vai por PR da
  branch da issue, mergeado no GitHub.
- Commits nessa branch começam com o prefixo `KAN-<numero>:` na mensagem.
- **1 branch/PR = 1 issue.** Se uma mudança acabar cobrindo mais de uma issue,
  abrir branch/PR separado para essa issue extra, ou pelo menos comentar e
  transicionar essa issue manualmente depois do merge.
- Nunca adicionar `Co-Authored-By` nas mensagens de commit.
