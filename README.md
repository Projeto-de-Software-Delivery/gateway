API Gateway do projeto de delivery.

Roteia `/clientes/**` para o serviço de cliente, `/lojas/**`, `/produtos/**` e
`/pedidos/**` para o serviço de loja e `/entregadores/**` para o serviço de
entregador, definidos em `gateway.routes.*` (`CLIENTE_SERVICE_URL`,
`LOJA_SERVICE_URL`, `ENTREGADOR_SERVICE_URL`).

Extraído do repo `backend` (KAN-59), onde vivia acoplado a um dos serviços
que roteia.

## Rate limiting

As rotas roteadas aceitam até `RATE_LIMIT_LIMITE` requisições (padrão
100) por IP a cada `RATE_LIMIT_JANELA` (padrão `1m`). Acima disso o gateway
responde `429` até a próxima janela. A contagem fica em memória, por instância.

## Logs de acesso e métricas

Toda requisição às rotas roteadas (inclusive as barradas com
`429`) gera uma linha de log com IP, método, path, serviço de destino, status e
duração, e é medida no timer `gateway.requisicoes`, com as tags `servico`,
`metodo` e `status`. As métricas ficam em `/actuator/metrics`, por exemplo
`/actuator/metrics/gateway.requisicoes?tag=servico:loja-service`.

## Documentação da API

Com a aplicação rodando: Swagger UI em `/swagger-ui.html`, OpenAPI JSON em
`/v3/api-docs`. Como o gateway só faz proxy, ele lista os prefixos que roteia
(`/clientes/**`, `/lojas/**`, `/produtos/**`, `/pedidos/**`, `/entregadores/**`) — as rotas de verdade estão documentadas no
serviço de destino de cada uma.
