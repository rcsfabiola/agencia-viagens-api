# API de Gerenciamento de Destinos de Viagem

Desafio da Unidade Curricular **Desenvolvimento de Sistemas Web** (SENAI) —
*Planejamento da arquitetura e desenvolvimento inicial de API REST*.

## 1. Visão geral do problema

Uma agência de viagens quer modernizar seus serviços digitais e disponibilizar
uma **API REST** para gerenciar informações sobre **destinos turísticos**
(nome, localização, descrição, disponibilidade de hotéis, atividades e
avaliações), permitindo que ela seja integrada futuramente a aplicativos de
turismo e parceiros comerciais.

Nesta primeira etapa, o foco é a **arquitetura da solução** e a construção
dos *endpoints* principais. Não há integração com banco de dados real nem
mecanismos avançados de segurança — os dados ficam armazenados em memória
enquanto a aplicação está rodando.

## 2. Arquitetura proposta

A aplicação segue uma **arquitetura em camadas**, com responsabilidades bem
separadas:

```text
Cliente (Postman, curl, app de turismo...)
  -> requisição HTTP ->
Controller — recebe a requisição, valida o formato e devolve a resposta
  -> chama ->
Service — contém as regras de negócio (buscar, filtrar, calcular média de avaliações, etc.)
  -> manipula ->
Model / Entity — representa o "Destino" e guarda os dados em memória
```

- **`controller/`** — `DestinoController`: expõe os endpoints REST, traduz
  HTTP em chamadas Java e devolve as respostas em JSON.
- **`service/`** — `DestinoService`: contém toda a lógica de negócio e o
  "banco de dados em memória" (um `Map`, já que o desafio não exige
  persistência real).
- **`model/`** — `Destino` (entidade) e `AvaliacaoRequest` (um pequeno DTO
  usado só para receber a nota de uma avaliação).
- **`exception/`** — tratamento de erros centralizado (`404` quando um
  destino não existe, `400` quando os dados enviados são inválidos).

### Estrutura do projeto

```text
agencia-viagens-api/
├── pom.xml
├── README.md
└── src/main/java/br/com/senai/agenciaviagens/
    ├── AgenciaViagensApiApplication.java   # ponto de entrada do Spring Boot
    ├── controller/
    │   └── DestinoController.java          # camada de controle (endpoints REST)
    ├── service/
    │   └── DestinoService.java             # camada de negócio + armazenamento em memória
    ├── model/
    │   ├── Destino.java                    # entidade de domínio
    │   └── AvaliacaoRequest.java           # DTO de entrada da avaliação
    └── exception/
        ├── DestinoNaoEncontradoException.java
        └── GlobalExceptionHandler.java     # tradução de exceções em respostas HTTP
```

### Por que essa separação?

Manter o Controller "burro" (só traduz HTTP) e o Service "inteligente" (só
regra de negócio) facilita testar, dar manutenção e, no futuro, trocar o
armazenamento em memória por um banco de dados real sem precisar mexer no
Controller.

### Decisões técnicas de consistência dos dados

Como os dados vivem em memória e o servidor atende várias requisições em
paralelo (uma *thread* por requisição), algumas decisões foram tomadas para
manter os dados consistentes:

- **`ConcurrentHashMap` + `AtomicLong`** — garantem que dois cadastros
  simultâneos não recebam o mesmo `id` nem corrompam o armazenamento.
- **`CopyOnWriteArrayList` para as avaliações** — uma lista comum
  (`ArrayList`) pode perder notas se dois `PATCH` chegarem ao mesmo tempo,
  porque o `add()` não é uma operação atômica.
- **Cópias defensivas nos *getters*** — `getAvaliacoes()` e
  `getAtividadesTuristicas()` devolvem cópias imutáveis, então nenhum código
  externo consegue alterar o estado interno do destino por fora das regras.
- **Campos somente-leitura** — `id` e `avaliacoes` são marcados como
  `READ_ONLY`: aparecem na resposta, mas são ignorados se o cliente tentar
  enviá-los no corpo da requisição. Isso impede que alguém cadastre um destino
  já com avaliações inventadas.
- **Regra de negócio junto do dado** — a validação "nota entre 1 e 5" vive
  dentro do método `Destino.adicionarAvaliacao()`, e não apenas na anotação do
  DTO, garantindo que ela valha para qualquer caminho de código.

## 3. Por que Java + Spring Boot?

- **Java** é fortemente tipado e amplamente usado no mercado corporativo,
  com grande comunidade e documentação — reduz erros e facilita manutenção
  em times maiores (útil pensando na integração com parceiros comerciais
  citada no desafio).
- **Spring Boot** foi escolhido porque:
  - Oferece um servidor web embutido (Tomcat), então não é preciso
    configurar infraestrutura para começar a testar a API.
  - Simplifica drasticamente a criação de endpoints REST com anotações
    (`@RestController`, `@GetMapping`, etc.), reduzindo código repetitivo.
  - Usa **Injeção de Dependência**/**Inversão de Controle** nativamente, o
    que deixa as camadas desacopladas (o Controller não cria o Service, ele
    apenas "recebe" um pronto pelo próprio Spring).
  - É o caminho natural de evolução: quando a agência decidir integrar um
    banco de dados de verdade, basta adicionar Spring Data JPA à camada de
    Service/Repository, sem reescrever o Controller.
  - **Spring Validation** foi incluído para validar os dados de entrada
    (ex.: impedir cadastrar um destino sem nome, ou uma avaliação fora da
    faixa de 1 a 5) de forma declarativa, evitando `if`s espalhados pelo
    código.
- O projeto foi gerado com o **Spring Initializr**, a ferramenta oficial
  para criar projetos Spring Boot, garantindo uma estrutura Maven padrão e
  confiável desde o início.

## 4. Endpoints da API

Prefixo base: `/api/destinos`

| Método | Rota | Descrição | Sucesso |
|---|---|---|---|
| `POST` | `/api/destinos` | Cadastra um novo destino | `201 Created` |
| `GET` | `/api/destinos` | Lista todos os destinos | `200 OK` |
| `GET` | `/api/destinos/buscar?nome=&localizacao=` | Pesquisa por nome e/ou localização (parâmetros opcionais) | `200 OK` |
| `GET` | `/api/destinos/{id}` | Detalha um destino específico | `200 OK` |
| `PUT` | `/api/destinos/{id}` | Atualiza os dados cadastrais de um destino | `200 OK` |
| `PATCH` | `/api/destinos/{id}/avaliacoes` | Registra uma nova avaliação (recalcula a média) | `200 OK` |
| `DELETE` | `/api/destinos/{id}` | Exclui um destino | `204 No Content` |

### Exemplo — cadastrar um destino

```bash
curl -X POST http://localhost:8080/api/destinos \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Bonito",
    "localizacao": "Mato Grosso do Sul, Brasil",
    "descricao": "Ecoturismo, rios cristalinos e grutas",
    "hoteisDisponiveis": 8,
    "atividadesTuristicas": ["Flutuação no Rio da Prata", "Gruta do Lago Azul"]
  }'
```

Resposta `201 Created`:

```json
{
  "id": 3,
  "nome": "Bonito",
  "localizacao": "Mato Grosso do Sul, Brasil",
  "descricao": "Ecoturismo, rios cristalinos e grutas",
  "hoteisDisponiveis": 8,
  "atividadesTuristicas": ["Flutuação no Rio da Prata", "Gruta do Lago Azul"],
  "avaliacoes": [],
  "mediaAvaliacoes": 0.0,
  "quantidadeAvaliacoes": 0
}
```

> O `id` é gerado pela API. Se o cliente enviar `id` ou `avaliacoes` no corpo,
> esses campos são ignorados — um destino sempre nasce sem avaliações.

### Exemplo — registrar uma avaliação

```bash
curl -X PATCH http://localhost:8080/api/destinos/1/avaliacoes \
  -H "Content-Type: application/json" \
  -d '{ "nota": 3 }'
```

Resposta `200 OK` (média recalculada automaticamente):

```json
{
  "id": 1,
  "nome": "Florianópolis",
  "localizacao": "Santa Catarina, Brasil",
  "descricao": "Ilha da Magia: praias, dunas e gastronomia",
  "hoteisDisponiveis": 12,
  "atividadesTuristicas": ["Trilha da Lagoinha do Leste", "Passeio de barco", "Surf na Joaquina"],
  "avaliacoes": [5, 4, 3],
  "mediaAvaliacoes": 4.0,
  "quantidadeAvaliacoes": 3
}
```

### Exemplo — pesquisar

```bash
curl "http://localhost:8080/api/destinos/buscar?localizacao=santa%20catarina"
```

A busca é **parcial e não diferencia maiúsculas de minúsculas**. Os dois
parâmetros são opcionais e podem ser combinados; sem nenhum parâmetro, a
pesquisa devolve todos os destinos.

### Exemplo — atualizar e excluir

```bash
curl -X PUT http://localhost:8080/api/destinos/3 \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Bonito",
    "localizacao": "MS, Brasil",
    "descricao": "Ecoturismo e flutuação",
    "hoteisDisponiveis": 10,
    "atividadesTuristicas": ["Flutuação no Rio da Prata"]
  }'

curl -X DELETE http://localhost:8080/api/destinos/3   # 204 No Content
```

### Tratamento de erros

Todos os erros devolvem um corpo JSON no mesmo formato, com `timestamp`,
`status` e `mensagem`:

| Situação | Status |
|---|---|
| `id` que não existe (buscar, atualizar, avaliar, excluir) | `404 Not Found` |
| Dados inválidos (destino sem nome, nota fora de 1–5, hotéis negativos) | `400 Bad Request` |
| `id` que não é um número (ex.: `/api/destinos/abc`) | `400 Bad Request` |
| Corpo ausente ou JSON malformado | `400 Bad Request` |
| Erro inesperado | `500 Internal Server Error` |

Destino inexistente (`404`):

```json
{
  "timestamp": "2026-08-30T17:09:04.574",
  "status": 404,
  "mensagem": "Destino com id 999 não foi encontrado"
}
```

Dados inválidos (`400`) — a resposta lista **cada campo** que falhou:

```json
{
  "timestamp": "2026-08-30T17:09:04.476",
  "status": 400,
  "mensagem": "Dados inválidos",
  "erros": {
    "nome": "O nome do destino e obrigatorio",
    "hoteisDisponiveis": "A quantidade de hoteis nao pode ser negativa"
  }
}
```

## 5. Como executar o projeto

Pré-requisitos: **JDK 17+** e **Maven** (o projeto já inclui o Maven
Wrapper, então não é obrigatório ter o Maven instalado separadamente).

```bash
cd agencia-viagens-api
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. A aplicação já nasce com 2 destinos
de exemplo cadastrados (Florianópolis e Gramado), para facilitar os
primeiros testes.

### Testando

Você pode testar com `curl` (como nos exemplos acima) ou com o **Postman**:

```bash
curl http://localhost:8080/api/destinos
```

## 6. Próximos passos (fora do escopo deste desafio)

- Persistir os dados em um banco real com Spring Data JPA.
- Adicionar autenticação/autorização (JWT, Spring Security).
- Adicionar paginação na listagem de destinos, caso a lista cresça muito.
- Separar DTOs de entrada e saída da entidade (`DestinoRequest` /
  `DestinoResponse`), isolando totalmente o contrato da API do modelo interno.
- Documentar a API com OpenAPI/Swagger.
