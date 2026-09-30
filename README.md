API Gateway do projeto de delivery.

Roteia `/clientes/**` para o serviço de cliente e `/lojas/**` para o serviço
de loja, definidos em `gateway.routes.*` (`CLIENTE_SERVICE_URL`,
`LOJA_SERVICE_URL`).

Extraído do repo `backend` (KAN-59), onde vivia acoplado a um dos serviços
que roteia.

## Rate limiting

As rotas roteadas (`/clientes/**` e `/lojas/**`) passam por um rate limiting
em memória (token bucket) por IP de origem. Cada cliente pode fazer uma rajada
de até `capacidade` requisições, e o balde se enche de novo em `janela`.
Estourado o limite, o gateway responde `429 Too Many Requests` com o header
`Retry-After`; toda resposta limitada traz `X-RateLimit-Limit` e
`X-RateLimit-Remaining`. O `/actuator` não é limitado.

| Propriedade                    | Variável                | Padrão |
|--------------------------------|-------------------------|--------|
| `gateway.rate-limit.habilitado`| `RATE_LIMIT_HABILITADO` | `true` |
| `gateway.rate-limit.capacidade`| `RATE_LIMIT_CAPACIDADE` | `100`  |
| `gateway.rate-limit.janela`    | `RATE_LIMIT_JANELA`     | `1m`   |

O estado fica na memória de cada instância, então com várias réplicas o limite
efetivo é multiplicado pelo número de réplicas.
