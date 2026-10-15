# Flash Sale Checkout

MVP didatico de checkout e reserva de estoque com dois microsservicos Java/Spring Boot, Redis local e RabbitMQ local. O projeto demonstra processamento assincrono, compensacao de estoque, cache-aside, bancos separados e rastreamento por correlation ID. Nao usa Docker nem servicos em nuvem.

## Arquitetura

- `checkout-service`: API REST, interface Thymeleaf, reserva de estoque no Redis, banco H2 proprio e consumidor de eventos.
- `order-processor-service`: consumidor de pedidos, banco H2 proprio e emissao de eventos de status/compensacao.
- `common-contracts`: contratos Java compartilhados das mensagens RabbitMQ; nao contem regras de dominio.
- RabbitMQ: broker local separado das aplicacoes Java.
- Redis: estoque e cache de consulta, com reserva Lua atomica e compensacao idempotente.

Filas duraveis:

| Fila | Produtor | Consumidor | Uso |
|---|---|---|---|
| `order-created` | Checkout | Order Processor | Processamento assincrono do pedido |
| `order-compensation` | Order Processor | Checkout | Devolucao do estoque apos falha |
| `order-status-updated` | Order Processor ou Checkout | Checkout | Atualizacao de `CONFIRMED` ou `CANCELLED` no banco/cache do Checkout |

O fluxo normal publica `order-created`, persiste o pedido no banco do Processor e publica `order-status-updated`. No fluxo de falha, o Processor persiste `CANCELLED`, publica `order-compensation`, e o Checkout devolve o estoque uma unica vez e atualiza o status local.

## Requisitos

- JDK 21 (64 bits).
- Maven 3.6.3 ou mais recente; recomenda-se Maven 3.9.x.
- RabbitMQ 4.3.6 e Erlang/OTP 28.x (64 bits), combinacao compativel de referencia.
- Redis local. No Windows, pode-se usar Redis pelo WSL2 ou um servidor Redis-protocol compatível aprovado pelo curso. Em Linux/macOS, use a instalacao local de Redis da plataforma.
- Portas livres: `5672` (AMQP), `15672` (painel RabbitMQ), `6379` (Redis) e `8080` (Checkout).

O JDK pode ser instalado em uma pasta de usuario se a politica da maquina permitir. Maven precisa acessar Maven Central para baixar dependencias na primeira compilacao. Em redes corporativas, configure no Maven o proxy/autenticacao fornecido pela TI; nao coloque credenciais no repositorio.

### RabbitMQ no Windows

1. Confira a [matriz oficial de compatibilidade Erlang/RabbitMQ](https://www.rabbitmq.com/docs/which-erlang) antes de baixar; a referencia deste README e RabbitMQ 4.3.6 com Erlang/OTP 28.x.
2. Instale Erlang/OTP 64-bit e RabbitMQ usando as instrucoes oficiais para [Windows](https://www.rabbitmq.com/docs/install-windows). A instalacao Windows do Erlang e a do servico RabbitMQ normalmente exigem permissao administrativa.
3. Habilite o painel de gerenciamento em um terminal:

   ```powershell
  rabbitmq-plugins.bat enable rabbitmq_management
  rabbitmqctl.bat status
   ```

4. Abra `http://localhost:15672` e entre com as credenciais locais configuradas. Em uma instalacao padrao de desenvolvimento, o usuario e senha podem ser `guest`/`guest`; mantenha essas credenciais apenas em `localhost`.

O instalador oficial normalmente registra um servico Windows e pode exigir permissao administrativa. Se isso for bloqueado, use outra maquina autorizada para a demonstracao ou solicite suporte da TI; nao substitua RabbitMQ por um broker diferente.

### Redis no Windows com WSL2

Em uma maquina onde WSL2 e permitido, abra PowerShell como administrador e instale uma distribuicao Linux:

```powershell
wsl --install -d Ubuntu
```

Conclua a inicializacao do Ubuntu e, no terminal Linux:

```bash
sudo apt update
sudo apt install redis-server
sudo service redis-server start
redis-cli ping
```

`redis-cli ping` deve responder `PONG`; o Windows acessa o Redis por `localhost:6379` pelo encaminhamento do WSL2. Se WSL2 nao estiver disponivel, confirme com o professor se um servidor Redis-protocol compativel para Windows e aceito no exercicio.

Antes de iniciar as aplicacoes, verifique os servicos locais:

```powershell
Test-NetConnection localhost -Port 5672
Test-NetConnection localhost -Port 6379
```

Ambos devem retornar `TcpTestSucceeded : True`.

## Compilar e testar

Na raiz da pasta extraida, execute `./scripts/setup.bat`. Ele verifica JDK/Maven e executa `mvn clean package` com settings proprios, ignorando mirrors corporativos e usando Maven Central:

```powershell
./scripts/setup.bat
```

O script requer acesso ao Maven Central na primeira execucao. Nao inclui Nexus nem credenciais corporativas. Se a rede do instrutor exigir proxy, configure o Maven com o proxy autorizado nessa maquina; nao versione credenciais. Para testar somente regras unitarias:

```powershell
./scripts/test.bat
```

Os testes unitarios cobrem validacao do fluxo de checkout, falta de estoque, falha de publicacao, consulta 404 apos tres tentativas, confirmacao e falha simulada. Eles nao iniciam RabbitMQ ou Redis.

Com RabbitMQ e Redis ativos, execute somente o teste ponta a ponta:

```powershell
./scripts/test.bat integracao
```

## Executar

Inicie Redis e RabbitMQ primeiro. Execute `./scripts/start.bat`; ele verifica Java, broker, Redis e JARs, depois abre uma janela para cada servico.

Para iniciar manualmente, use dois terminais na raiz do projeto. O build ja deve ter sido feito por `./scripts/setup.bat`.

```powershell
Set-Location .\checkout-service
java -jar .\target\checkout-service-1.0.0-SNAPSHOT.jar
```

Terminal 2, iniciar Order Processor:

```powershell
Set-Location .\order-processor-service
java -jar .\target\order-processor-service-1.0.0-SNAPSHOT.jar
```

Checkout: `http://localhost:8080/`

Painel RabbitMQ: `http://localhost:15672/`

O Hibernate cria/atualiza os bancos em `checkout-service/checkout-db` e `order-processor-service/processor-db`. Cada servico acessa somente seu proprio banco.

## Demonstracao visual

Na pagina `http://localhost:8080/`:

1. Cadastre o estoque de `PROD-001`, por exemplo 10 unidades.
2. Envie um pedido com a opcao de falha desmarcada. O status deve mudar de `PROCESSING` para `CONFIRMED`.
3. Envie outro pedido com a opcao de falha marcada. A pagina exibe o motivo, o pedido chega a `CANCELLED` e as unidades reservadas voltam ao estoque.
4. Use o identificador apresentado para consultar o pedido e acompanhe o `correlation-id` nos logs dos dois servicos.

## Demonstracao automatizada via HTTP

Com os dois servicos e Redis/RabbitMQ ativos, execute na raiz:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\scripts\run-demo.ps1
```

O script prepara o estoque, envia os cenarios de sucesso e falha, aguarda os status finais e confere que o estoque foi devolvido. Para escolher outro produto ou URL:

```powershell
.\scripts\run-demo.ps1 -BaseUrl http://localhost:8080 -ProductId PROD-002
```

## API principal

### Criar pedido

`POST /v1/orders` retorna `202 Accepted` quando o estoque foi reservado e o RabbitMQ confirmou a publicacao, ou `409 Conflict` quando o saldo e insuficiente.

```json
{
  "produto_id": "PROD-001",
  "quantidade": 2,
  "cliente_cpf": "50329291076",
  "cliente_nome": "Cliente de demonstracao",
  "simular_falha": false,
  "motivo_falha": "Falha simulada para demonstracao"
}
```

Para testar falha/compensacao, use `"simular_falha": true`. O campo opcional `X-Correlation-Id` pode ser enviado pelo cliente; se ausente ou invalido, o Checkout gera um UUID.

### Estoque

- `PUT /v1/inventory/{produto_id}` com `{"quantity": 10}` define estoque para a demonstracao.
- `GET /v1/inventory/{produto_id}` consulta o saldo atual.

### Consultar pedido

- `GET /v1/orders/{pedido_id}` consulta primeiro o Redis (TTL de cinco minutos) e, em cache miss, consulta o banco do Checkout.
- Sao feitas ate tres tentativas imediatas antes do `404 Not Found`.

## Logs e rastreamento

Cada aplicacao escreve no console e em:

- `checkout-service/logs/checkout-service.log`
- `order-processor-service/logs/order-processor-service.log`

Os registros incluem `correlationId` no MDC. Pesquise pelo mesmo ID nos dois arquivos para acompanhar a reserva, publicacao, consumo, persistencia, confirmacao ou compensacao. Eventos de negocio incluem `order_published`, `order_received`, `order_confirmed`, `order_processing_failed`, `stock_compensated` e `order_status_updated`.

## Preparar arquivo ZIP

Antes de compactar o projeto, execute `./scripts/clean-for-zip.bat` e confirme com `S`. O script remove apenas pastas `target`, bancos H2 locais e logs gerados. Nao apaga fontes, README, scripts, configuracoes ou cache Maven global. Depois, compacte a pasta `flash-sale` inteira. Na outra maquina, extraia a pasta, instale/configure os pre-requisitos, execute `./scripts/setup.bat`, inicie RabbitMQ e Redis localmente e depois execute `./scripts/start.bat`.

## Limites do MVP

- H2 em arquivo e adequado para demonstracao local; nao representa uma configuracao de banco de producao.
- O fluxo usa confirmacao de publicacao, filas duraveis, retries locais e tratamento idempotente da compensacao. Nao implementa Outbox transacional nem garantia de exatamente-uma-vez.
- A fila `order-status-updated` e uma extensao explicita ao item 7: mantem os bancos segregados sem fazer o Checkout consultar diretamente o banco do Processor.
- O teste de integracao ponta a ponta depende de RabbitMQ e Redis locais ativos; sem esses servicos, execute os testes unitarios e use o roteiro acima em outra maquina.