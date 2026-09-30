API Gateway do projeto de delivery.

Roteia `/clientes/**` para o serviço de cliente e `/lojas/**` para o serviço
de loja, definidos em `gateway.routes.*` (`CLIENTE_SERVICE_URL`,
`LOJA_SERVICE_URL`).

Extraído do repo `backend` (KAN-59), onde vivia acoplado a um dos serviços
que roteia.

## Autenticação e autorização

O gateway é um resource server OAuth2: `/clientes/**` e `/lojas/**` exigem
`Authorization: Bearer <JWT>`, assinado em HS256 com `JWT_SECRET` (o mesmo
segredo dos outros serviços, com pelo menos 32 caracteres) e com as claims
`sub` e `role` (`cliente` | `loja` | `entregador`). Sem token, ou com token
inválido/expirado, a resposta é `401`; com papel sem permissão, `403`.

| Rota | Quem acessa |
|------|-------------|
| `/clientes/**` | `cliente` |
| `GET /lojas/**` | `cliente`, `loja`, `entregador` |
| demais métodos em `/lojas/**` | `loja` |

O token é repassado no `Authorization` para o serviço de destino. `/actuator/**`,
o Swagger UI e o `/v3/api-docs` continuam abertos.

## Rate limiting

`/clientes/**` e `/lojas/**` aceitam até `RATE_LIMIT_LIMITE` requisições (padrão
100) por IP a cada `RATE_LIMIT_JANELA` (padrão `1m`). Acima disso o gateway
responde `429` até a próxima janela. A contagem fica em memória, por instância.

## Logs de acesso e métricas

Toda requisição a `/clientes/**` e `/lojas/**` (inclusive as barradas com
`429`) gera uma linha de log com IP, método, path, serviço de destino, status e
duração, e é medida no timer `gateway.requisicoes`, com as tags `servico`,
`metodo` e `status`. As métricas ficam em `/actuator/metrics`, por exemplo
`/actuator/metrics/gateway.requisicoes?tag=servico:loja-service`.

## Documentação da API

Com a aplicação rodando: Swagger UI em `/swagger-ui.html`, OpenAPI JSON em
`/v3/api-docs`. Como o gateway só faz proxy, ele lista os prefixos que roteia
(`/clientes/**`, `/lojas/**`) — as rotas de verdade estão documentadas no
serviço de destino de cada uma.
