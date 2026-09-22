# Spec: MusicBox — Backend local (Ktor + SQLite) + App Android (Clean Architecture + MVI)

- **Data:** 2026-09-21
- **Status:** design aprovado em conversa; este documento aguarda revisão do usuário
- **Projetos envolvidos:** `MusicBox` (app Android, existente) e `MusicBox-backend` (novo, subpasta do repo — mesmo git)

---

## 1. Contexto e objetivo

Hoje o app MusicBox lê uma lista de 10 `Music` hardcoded em `MusicRepository.kt` com `delay(5000)` simulando rede. O README do projeto já prevê a evolução "Mockoon → API real".

**Objetivo:** criar um backend local na máquina do desenvolvedor que sirva esse catálogo via REST, e refazer a camada de apresentação/dados do app em **Clean Architecture + MVI**, de modo que o app consuma a API real sem acoplamento entre camadas.

**Sucesso =** backend no ar com CRUD funcional (verificado por testes + curl) e app renderizando a lista/detalhe vindos da API, com estados de erro/retry funcionando.

## 2. Escopo

### Em escopo
- Novo projeto `MusicBox-backend` (Ktor + Exposed + SQLite) com API REST **CRUD completa**
- Seed dos 10 itens atuais no primeiro boot
- Refatoração do app para Clean Architecture (domain/data/presentation) + MVI (State/Intent/Effect)
- Consumo da API via Retrofit no app, com modelos próprios por camada e mappers
- Liberação de HTTP local no Android (`network_security_config`)
- Testes: backend (ktor-server-test-host), unitários do app (mappers/usecase/ViewModels), verificação manual E2E

### Fora de escopo (YAGNI — decisões aprovadas)
- Autenticação, HTTPS, CORS, paginação, deploy, painel/admin UI
- Hilt (usa-se DI manual), Ktor Client no app (usa-se Retrofit — ver §8)
- Uso de CRUD pelo app (o app só lê; escrita via Insomnia/curl)
- Atualização de outras dependências desatualizadas do app (exceto `lifecycle` 2.6.1 → 2.8.7)
- Mudanças de tema (`Theme.kt`/dynamic color) e demais itens do code review não ligados a este trabalho

## 3. Backend — `MusicBox-backend`

### 3.1 Stack e versões (confirmadas em Maven Central/docs)

| Componente | Versão |
|---|---|
| Kotlin (JVM plugin) | 2.2.20 |
| Ktor (server CIO) | 3.5.2 |
| Exposed (core + jdbc) | 1.5.0 |
| sqlite-jdbc | 3.50.2.0 |
| kotlinx-serialization (plugin) | 2.2.20 (mesmo do Kotlin) |
| logback-classic | 1.5.16 |
| Gradle wrapper | 8.13 (copiado do MusicBox) |
| JDK runtime | ≥ 17 (verificar `java`/Android Studio JBR) |

Estrutura de build: `settings.gradle.kts` + `build.gradle.kts` (application plugin, `mainClass`) + `gradle/libs.versions.toml`.

### 3.2 Estrutura de fontes

```
MusicBox-backend/src/main/kotlin/com/gcherubini/musicbox/backend/
├── Application.kt          ← fun main(): embeddedServer(CIO, port=8080, host="0.0.0.0")
│                              plugins: ContentNegotiation(json prettyPrint), StatusPages
├── model/Music.kt          ← @Serializable (espelho 1:1 do Music do domínio do app)
├── database/
│   ├── DatabaseFactory.kt  ← Database.connect("jdbc:sqlite:data/musicbox.db"), cria schema, seed se vazio
│   ├── Musics.kt           ← object Musics : Table("musics")
│   └── MusicDao.kt         ← getAll, getById, create, update, delete
├── routes/MusicRoutes.kt   ← fun Route.musicRoutes()
└── seed/MusicSeeder.kt     ← os 10 itens atuais de MusicRepository.kt, copiados verbatim
```

### 3.3 API REST

| Rota | Corpo | Sucesso | Erros |
|---|---|---|---|
| `GET /musics` | — | **200** `Music[]` (ordem de inserção) | 500 |
| `GET /musics/{id}` | — | **200** `Music` | **404** |
| `POST /musics` | `Music` (id opcional) | **201** + `Music` criado | **400** validação, **409** id já existe |
| `PUT /musics/{id}` | `Music` completo | **200** `Music` atualizado | **400**, **404** |
| `DELETE /musics/{id}` | — | **204** sem corpo | **404** |

- `GET /` responde `200` texto "MusicBox API" (health check simples).

### 3.4 Regras de negócio

- **Campos obrigatórios:** `title`, `artist`, `label`, `releaseDate`, `genre`, `coverImageUrl`. **Opcional:** `spotifyTrack` (nullable; `null` no PUT limpa o valor).
- **POST com `id`:** usa o id enviado (se já existir → 409). **POST sem `id` (vazio/nulo):** servidor gera UUID.
- **PUT:** substituição total do registro (exceto `id`, que vem da rota).
- **Seed:** executa somente se `Musics.selectAll().count() == 0` no boot.
- **Schema:** colunas `varchar` (`id` 64 PK, `title`/`artist`/`label` 255, `releaseDate` 32, `genre` 128, `coverImageUrl` 512, `spotify_track` 512 nullable).
- **Banco:** arquivo `data/musicbox.db` relativo ao diretório do projeto; pasta `data/` no `.gitignore` do backend.

### 3.5 Formato de erro

Todo erro responde JSON: `{ "error": "<mensagem>" }` com o status HTTP correspondente (400/404/409/500), montado pelo `StatusPages` (inclui `exception<Throwable>` → 500 com log).

### 3.6 Concorrência

JDBC é blocking: toda chamada ao `MusicDao` nas rotas ocorre dentro de `withContext(Dispatchers.IO)` para não travar o event loop do Ktor.

## 4. App Android — Clean Architecture + MVI

### 4.1 Regra de dependência

```
presentation ──▶ domain ◀── data
```

- `domain`: modelos, interfaces de repositório, use cases. **Não** importa Android/Retrofit/JSON/DTO.
- `presentation`: Compose + contratos MVI + ViewModels. **Não** importa nada de `data`.
- `data`: Retrofit, DTOs, mappers, `MusicRepositoryImpl`. Só é citado na composição de DI.

### 4.2 Pacotes (substitui a estrutura atual)

```
com.gcherubini.musicbox/
├── di/AppContainer.kt
├── domain/
│   ├── model/Music.kt                     ← id, title, coverImageUrl, label, releaseDate, genre, artist, spotifyTrack?
│   ├── repository/MusicRepository.kt      ← interface: getMusics(): List<Music>; getMusicById(id): Music?
│   └── usecase/
│       ├── GetMusicsUseCase.kt            ← operator fun invoke(): List<Music>
│       └── GetMusicByIdUseCase.kt         ← operator fun invoke(id: String): Music?
├── data/
│   ├── remote/api/
│   │   ├── ApiConfig.kt                   ← const BASE_URL = "http://10.0.2.2:8080/"
│   │   └── MusicApi.kt                    ← interface Retrofit (@GET("musics"), @GET("musics/{id}"))
│   ├── remote/dto/MusicDto.kt             ← @Serializable (JSON)
│   ├── mapper/MusicMappers.kt             ← MusicDto.toDomain(), Music.toUiModel()
│   └── repository/MusicRepositoryImpl.kt  ← : MusicRepository
├── presentation/
│   ├── MainActivity.kt
│   ├── navigation/MusicBoxNavHost.kt      ← recebe AppContainer por parâmetro
│   ├── theme/ (inalterado)
│   ├── welcome/WelcomeScreen.kt           ← sem estado → sem ViewModel
│   ├── musiclist/
│   │   ├── MusicListContract.kt           ← Intent / UiState / Effect
│   │   ├── MusicListViewModel.kt
│   │   └── MusicListScreen.kt
│   └── detail/
│       ├── DetailContract.kt
│       ├── DetailViewModel.kt
│       └── DetailScreen.kt
```

**Arquivos removidos:** `viewmodel/MusicViewModel.kt`, `repository/MusicRepository.kt` (hardcoded), `model/Music.kt` (substituído pelos 3 modelos), `screens/*` (movidos/reescritos em `presentation/*`).

### 4.3 Modelos por camada

| Camada | Modelo | Papel |
|---|---|---|
| data | `MusicDto` | JSON exatamente como o backend devolve (`@Serializable`) |
| domain | `Music` | contrato interno do app |
| presentation | `MusicUiModel` | o que a UI renderiza (desacoplada do domínio); espelha os campos do domínio: `id, title, artist, label, releaseDate, genre, coverImageUrl, spotifyTrack` |

Mappers: `MusicDto.toDomain(): Music`, `Music.toUiModel(): MusicUiModel`. **Nenhum cast ou acesso cruzado de campos entre camadas.**

### 4.4 Contratos MVI

**Lista (`MusicListContract.kt`):**
```kotlin
sealed interface MusicListIntent {
    data object Retry : MusicListIntent
    data class MusicClicked(val id: String) : MusicListIntent
}
sealed interface MusicListUiState {
    data object Loading : MusicListUiState
    data class Error(val message: String) : MusicListUiState
    data class Success(val musics: List<MusicUiModel>) : MusicListUiState
}
sealed interface MusicListEffect {
    data class OpenDetail(val id: String) : MusicListEffect   // one-shot, via Channel
}
```
- `MusicListViewModel(getMusicsUseCase)`: `init { load() }`; `onIntent(intent)` único ponto de entrada; efeitos expostos como `Flow<MusicListEffect>` (Channel → `receiveAsFlow()`).
- A UI coleta estado com `collectAsStateWithLifecycle()` e efeitos em `LaunchedEffect { effects.collect { ... } }`.

**Detalhe (`DetailContract.kt`):**
```kotlin
sealed interface DetailIntent { data object Retry : DetailIntent }
sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Error(val message: String) : DetailUiState
    data class Success(val music: MusicUiModel) : DetailUiState
}
```
- `DetailViewModel(musicId, getMusicByIdUseCase)`: `init { load() }` — **resolve o bug da tela branca** (não depende do estado da lista; id chega via factory a partir dos nav args).

**Welcome:** sem contrato/VM (apenas `onExploreClick` lambda).

### 4.5 Navegação

- Rotas inalteradas: `welcome`, `music_list`, `music_detail/{musicId}`.
- `navController.navigate(Screen.MusicDetail.createRoute(id))` (usa `createRoute`, sem string literal).
- Argumento lido com `MUSIC_DETAIL_ARGUMENT_ID` (sem `"musicId"` hardcoded).
- VMs com escoping por backstack entry (lista sobrevive à ida/volta ao detalhe; detalhe é criado a cada navegação).

### 4.6 DI manual

- `AppContainer` instancia: `MusicApi` (Retrofit) → `MusicRepositoryImpl` → use cases → factories de VM (`viewModelFactory { initializer { ... } }`).
- Criado no `MainActivity` e passado como parâmetro para `MusicBoxNavHost`.
- Sem Hilt/KSP/Dagger.

### 4.7 Mudanças de build (`gradle/libs.versions.toml` + `app/build.gradle.kts`)

Adicionar:
- plugin `kotlin-serialization` (version.ref = kotlin 2.0.21) e aplicar no módulo app
- `com.squareup.retrofit2:retrofit:2.11.0`
- `com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0`
- `org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3`
- `androidx.lifecycle:lifecycle-viewmodel-compose` e `androidx.lifecycle:lifecycle-runtime-compose`
- atualizar `lifecycle` 2.6.1 → **2.8.7**

### 4.8 Segurança de rede

- Novo `app/src/main/res/xml/network_security_config.xml` permitindo cleartext para `10.0.2.2`, `localhost`, `127.0.0.1`.
- Atributo `android:networkSecurityConfig` no `<application>` do manifesto.
- `INTERNET` já existe ✓.

## 5. Fluxos de dados

```
[List]  UI --Intent(MusicClicked/Retry)--> ViewModel -->> GetMusicsUseCase
              >> MusicRepository(interface, domain) << MusicRepositoryImpl(data)
              << GET /musics << Retrofit/MusicApi
              << MusicDto --toDomain--> Music --toUiModel--> MusicUiModel
              << UiState (StateFlow) --> collectAsStateWithLifecycle --> recomposição
        Effect(OpenDetail) --Channel--> navegação (dispara 1x)

[Detail] nav arg id --> DetailViewModel.init --> GetMusicByIdUseCase --> mesma cadeia
         id inexistente/404 --> UiState.Error("Música não encontrada") + Retry
```

## 6. Tratamento de erros

- **Backend:** ver §3.5 (JSON `{"error"}` + status).
- **App:** falha HTTP (não-2xx) → `HttpException`; falha de rede → `IOException`; JSON inválido → `SerializationException`. Todas são `Exception`, capturadas no ViewModel, que emite `UiState.Error(message)` com botão Retry. **Presentation/domain nunca dependem de tipos Retrofit/OkHttp.**
- Mensagens: usar `e.message` quando existir; fallback `"Erro desconhecido"`.

## 7. Testes e verificação

1. **Backend (automático):** `ktor-server-test-host` cobrindo: GET lista, GET por id (200/404), POST (201/400/409), PUT (200/400/404), DELETE (204/404).
2. **App (unit JUnit):** mappers (Dto→domain→ui), `GetMusicsUseCase`/`GetMusicByIdUseCase` com repositório fake, `MusicListViewModel` e `DetailViewModel` (intenção → transição de estado; effect emitido 1x).
3. **Manual E2E:**
   - `MusicBox-backend`: `gradlew run` + `curl` nos 5 endpoints;
   - `MusicBox`: `gradlew assembleDebug` compila; app no emulador com backend no ar mostra a lista vinda da API; detalhe abre com Retry em caso de id inexistente;
   - **caminho de falha:** backend parado → tela de Error → Retry após subir o backend → Success.

## 8. Riscos e decisões técnicas registradas

- **Ktor Client descartado no app:** Ktor 3.3+ é compilado com Kotlin 2.2 e o app usa compiler 2.0.21 → risco real de erro "incompatible version of Kotlin". Retrofit (Java) + serialization 1.7.3 (casa com 2.0.21) evita isso sem mexer no build Compose existente.
- **Exposed 1.5.0 muda pacotes** (`org.jetbrains.exposed.v1.jdbc.*`, `org.jetbrains.exposed.v1.sql.*`) — difere de tutoriais 0.x antigos.
- **JDK:** backend exige JDK ≥ 17 em runtime; confirmar `java` disponível na máquina (hoje `JAVA_HOME` vazio) — usar JBR do Android Studio se necessário.
- **Seed verbatim:** dados copiados do `MusicRepository.kt` atual antes de removê-lo.

## 9. Critérios de aceite

- [ ] `MusicBox-backend` sobe em `0.0.0.0:8080` e cria `data/musicbox.db` com seed de 10 itens
- [ ] CRUD respondendo conforme a tabela §3.3, erros em JSON §3.5
- [ ] Testes do backend verdes
- [ ] App sem imports de `data` em `presentation` (regra §4.1) e sem imports de Android em `domain`
- [ ] Contratos MVI com Intent/UiState/Effect por tela (lista e detalhe)
- [ ] 3 modelos + mappers conforme §4.3
- [ ] Unit tests do app verdes; `assembleDebug` compila
- [ ] E2E manual: lista e detalhe vindo da API; falha de rede mostra Error com Retry
