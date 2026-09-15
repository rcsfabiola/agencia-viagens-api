# API de Gerenciamento de Destinos de Viagem

Desafio da Unidade Curricular **Desenvolvimento de Sistemas Web** (SENAI) —
evoluído do desafio inicial (*Planejamento da arquitetura e desenvolvimento
inicial de API REST*) para o Desafio 2 (*Evolução da API com banco de dados
e segurança*).

## 1. Visão geral do problema

Uma agência de viagens quer modernizar seus serviços digitais e disponibilizar
uma **API REST** para gerenciar informações sobre **destinos turísticos**
(nome, localização, descrição, disponibilidade de hotéis, atividades e
avaliações), permitindo que ela seja integrada futuramente a aplicativos de
turismo e parceiros comerciais.

Na primeira versão (Desafio 1), os dados viviam em memória e não havia
controle de acesso. Nesta segunda versão (Desafio 2), a API foi evoluída
para se aproximar de um ambiente real de produção:

- os dados agora são **persistidos em um banco de dados PostgreSQL**, via
  Spring Data JPA;
- a API agora exige **autenticação** (login) e aplica **autorização por
  perfil de acesso** (`ADMIN` e `USER`), protegendo as operações sensíveis.

## 2. Arquitetura proposta

A aplicação segue uma **arquitetura em camadas**, com responsabilidades bem
separadas:

```text
Cliente (Postman, Thunder Client, app de turismo...)
  -> requisição HTTP (com login) ->
Security — autentica o usuário e verifica se o perfil dele pode acessar aquela rota
  -> Controller — recebe a requisição, valida o formato e devolve a resposta
  -> chama ->
Service — contém as regras de negócio (buscar, filtrar, calcular média de avaliações, etc.)
  -> chama ->
Repository (Spring Data JPA) — acessa o banco de dados
  -> persiste em ->
PostgreSQL
```

- **`controller/`** — `DestinoController`: expõe os endpoints REST, traduz
  HTTP em chamadas Java e devolve as respostas em JSON.
- **`service/`** — `DestinoService` e `UsuarioService`: contêm toda a lógica
  de negócio, agora operando sobre dados persistidos no banco (em vez de
  um `Map` em memória).
- **`repository/`** — `DestinoRepository` e `UsuarioRepository`: interfaces
  do Spring Data JPA que dão acesso ao banco sem exigir SQL manual.
- **`model/`** — `Destino`, `Usuario`, `Perfil` (entidades JPA) e
  `AvaliacaoRequest` (DTO usado para receber a nota de uma avaliação).
- **`security/`** — `SecurityConfig`: configura autenticação (Basic Auth)
  e as regras de autorização por perfil.
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
    │   ├── DestinoService.java             # camada de negócio de destinos (persistência via JPA)
    │   └── UsuarioService.java             # popula os usuários de teste no banco
    ├── repository/
    │   ├── DestinoRepository.java          # acesso a dados de Destino (Spring Data JPA)
    │   └── UsuarioRepository.java          # acesso a dados de Usuario (Spring Data JPA)
    ├── security/
    │   └── SecurityConfig.java             # autenticação e autorização (Spring Security)
    ├── model/
    │   ├── Destino.java                    # entidade JPA
    │   ├── Usuario.java                    # entidade JPA (implementa UserDetails)
    │   ├── Perfil.java                     # enum ADMIN / USER
    │   └── AvaliacaoRequest.java           # DTO de entrada da avaliação
    └── exception/
        ├── DestinoNaoEncontradoException.java
        └── GlobalExceptionHandler.java     # tradução de exceções em respostas HTTP
```

### Por que essa separação?

Manter o Controller "burro" (só traduz HTTP), o Service "inteligente" (só
regra de negócio) e o Repository responsável só pelo acesso a dados facilita
testar, dar manutenção e trocar peças isoladamente — foi exatamente essa
separação que permitiu evoluir do armazenamento em memória (Desafio 1) para
persistência real em banco (Desafio 2) sem reescrever o Controller.

## 3. Por que Java + Spring Boot + PostgreSQL + Spring Security?

- **Java + Spring Boot**: ver justificativa original — fortemente tipado,
  amplamente usado no mercado, servidor web embutido, injeção de
  dependência nativa, reduz código repetitivo com anotações.
- **Spring Data JPA + PostgreSQL**: Spring Data JPA elimina a necessidade de
  escrever SQL manual para operações básicas (salvar, buscar, listar,
  excluir) — basta declarar interfaces que estendem `JpaRepository`. O
  Hibernate (implementação de JPA usada pelo Spring Boot) traduz
  automaticamente objetos Java em linhas de tabelas relacionais.
  PostgreSQL foi escolhido por ser um banco relacional robusto, gratuito e
  amplamente usado no mercado, com ótimo suporte no ecossistema Spring.
- **Spring Security**: biblioteca padrão do ecossistema Spring para
  autenticação e autorização. Foi usada com **HTTP Basic Auth** (usuário e
  senha enviados em cada requisição) por ser simples de configurar e testar
  via Postman/Thunder Client, adequado ao escopo do desafio. As senhas são
  armazenadas com hash **BCrypt**, nunca em texto puro.

## 4. Configuração do banco de dados

A API espera um banco PostgreSQL local chamado `agencia_viagens`, acessado
por um usuário dedicado `agencia_user`.

### Criar o banco e o usuário

Conecte-se ao PostgreSQL como superusuário e rode:

```sql
CREATE DATABASE agencia_viagens;
CREATE USER agencia_user WITH PASSWORD 'sua_senha_aqui';
GRANT ALL PRIVILEGES ON DATABASE agencia_viagens TO agencia_user;
```

Depois, conectado especificamente ao banco `agencia_viagens` (`\c agencia_viagens`),
conceda também permissão de criar tabelas no schema `public` (necessário a
partir do PostgreSQL 15):

```sql
GRANT ALL ON SCHEMA public TO agencia_user;
```

### Configurar a aplicação

No arquivo `src/main/resources/application.properties`, configure a conexão:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/agencia_viagens
spring.datasource.username=agencia_user
spring.datasource.password=sua_senha_aqui
spring.jpa.hibernate.ddl-auto=update
```

Com `ddl-auto=update`, o Hibernate cria e atualiza as tabelas (`destinos`,
`destino_atividades`, `destino_avaliacoes`, `usuarios`) automaticamente na
primeira execução — não é preciso rodar nenhum script SQL manualmente.

## 5. Autenticação e autorização

A API usa **HTTP Basic Auth**: usuário e senha são enviados em cada
requisição (via cabeçalho `Authorization`, ou pelo campo "Basic Auth" do
Postman/Thunder Client).

### Usuários de teste

Na primeira execução, dois usuários são criados automaticamente no banco:

| Username | Senha | Perfil |
|---|---|---|
| `admin` | `admin123` | `ADMIN` |
| `user` | `user123` | `USER` |

### Regras de acesso por perfil

| Operação | Endpoint | Quem pode acessar |
|---|---|---|
| Listar, pesquisar, detalhar | `GET /api/destinos/**` | `USER` e `ADMIN` |
| Avaliar um destino | `PATCH /api/destinos/{id}/avaliacoes` | `USER` e `ADMIN` |
| Cadastrar | `POST /api/destinos` | Somente `ADMIN` |
| Atualizar | `PUT /api/destinos/{id}` | Somente `ADMIN` |
| Excluir | `DELETE /api/destinos/{id}` | Somente `ADMIN` |

Qualquer requisição sem autenticação recebe `401 Unauthorized`. Uma
requisição autenticada, mas com perfil sem permissão para aquela ação,
recebe `403 Forbidden`.

## 6. Endpoints da API

Prefixo base: `/api/destinos`

| Método | Rota | Descrição | Sucesso |
|---|---|---|---|
| `POST` | `/api/destinos` | Cadastra um novo destino (ADMIN) | `201 Created` |
| `GET` | `/api/destinos` | Lista todos os destinos | `200 OK` |
| `GET` | `/api/destinos/buscar?nome=&localizacao=` | Pesquisa por nome e/ou localização (parâmetros opcionais) | `200 OK` |
| `GET` | `/api/destinos/{id}` | Detalha um destino específico | `200 OK` |
| `PUT` | `/api/destinos/{id}` | Atualiza os dados cadastrais de um destino (ADMIN) | `200 OK` |
| `PATCH` | `/api/destinos/{id}/avaliacoes` | Registra uma nova avaliação (recalcula a média) | `200 OK` |
| `DELETE` | `/api/destinos/{id}` | Exclui um destino (ADMIN) | `204 No Content` |

### Exemplo — cadastrar um destino (como ADMIN)

```bash
curl -X POST http://localhost:8080/api/destinos \
  -u admin:admin123 \
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

> O `id` é gerado pelo banco. Se o cliente enviar `id` ou `avaliacoes` no
> corpo, esses campos são ignorados — um destino sempre nasce sem avaliações.

### Exemplo — registrar uma avaliação (como USER)

```bash
curl -X PATCH http://localhost:8080/api/destinos/1/avaliacoes \
  -u user:user123 \
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

### Exemplo — pesquisar (autenticado, qualquer perfil)

```bash
curl -u user:user123 "http://localhost:8080/api/destinos/buscar?localizacao=santa%20catarina"
```

A busca é **parcial e não diferencia maiúsculas de minúsculas**. Os dois
parâmetros são opcionais e podem ser combinados; sem nenhum parâmetro, a
pesquisa devolve todos os destinos.

### Exemplo — atualizar e excluir (como ADMIN)

```bash
curl -X PUT http://localhost:8080/api/destinos/3 \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Bonito",
    "localizacao": "MS, Brasil",
    "descricao": "Ecoturismo e flutuação",
    "hoteisDisponiveis": 10,
    "atividadesTuristicas": ["Flutuação no Rio da Prata"]
  }'

curl -X DELETE http://localhost:8080/api/destinos/3 -u admin:admin123   # 204 No Content
```

### Tratamento de erros

Todos os erros devolvem um corpo JSON no mesmo formato, com `timestamp`,
`status` e `mensagem`:

| Situação | Status |
|---|---|
| Sem autenticação | `401 Unauthorized` |
| Autenticado, mas sem permissão para a ação | `403 Forbidden` |
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

## 7. Como executar o projeto

Pré-requisitos: **JDK 17+**, **Maven** (o projeto já inclui o Maven Wrapper)
e **PostgreSQL 15+** rodando localmente.

1. Crie o banco e o usuário conforme a seção [Configuração do banco de
   dados](#4-configuração-do-banco-de-dados).
2. Configure `src/main/resources/application.properties` com suas
   credenciais.
3. Rode a aplicação:

```bash
cd agencia-viagens-api
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. Na primeira execução, o Hibernate
cria as tabelas automaticamente e a aplicação popula 2 destinos de exemplo
(Florianópolis e Gramado) e os 2 usuários de teste (`admin` e `user`) — ver
seção [Autenticação e autorização](#5-autenticação-e-autorização).

### Testando

Você pode testar com `curl` (como nos exemplos acima, usando `-u
usuario:senha`) ou com o **Postman**/**Thunder Client**, usando a aba de
autenticação **Basic Auth**.

```bash
curl -u user:user123 http://localhost:8080/api/destinos
```

## 8. Próximos passos (fora do escopo deste desafio)

- Adicionar paginação na listagem de destinos, caso a lista cresça muito.
- Separar DTOs de entrada e saída da entidade (`DestinoRequest` /
  `DestinoResponse`), isolando totalmente o contrato da API do modelo interno.
- Documentar a API com OpenAPI/Swagger.
- Evoluir a autenticação de HTTP Basic para tokens JWT, evitando reenviar
  usuário e senha em toda requisição.
- Permitir cadastro de novos usuários via endpoint próprio (hoje eles só
  existem via dados de teste pré-carregados).