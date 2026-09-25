# Gestão de Toner — MVP Acadêmico

Aplicação desktop em Java Swing para registrar e acompanhar solicitações de toner.

## Objetivo

Centralizar o registro de solicitações de toner, acompanhar o status dos pedidos e organizar informações relacionadas ao atendimento, reduzindo controles manuais e facilitando a consulta do histórico.

## Funcionalidades demonstradas

- cadastro e acompanhamento de solicitações;
- status **Aguardando Aprovação**, **A Caminho** e **Entregue**;
- pesquisa e filtros;
- seleção de impressora/modelo de toner;
- seleção de cores para impressoras coloridas;
- contador de impressão com dados demonstrativos;
- registro de nota fiscal, tipo de envio e código de rastreio;
- controle de estoque;
- exportação de pedidos em CSV e impressão da lista;
- configuração opcional de integração via Power Automate;
- envio opcional de anexos pelo fluxo configurado.

## Privacidade e segurança

Este repositório **não contém dados reais da operação**. Foram removidos ou substituídos:

- históricos de pedidos reais;
- endereços IP e identificadores de equipamentos internos;
- nomes de colaboradores usados na operação;
- endereços de e-mail corporativos;
- URLs reais de Power Automate, credenciais e configurações locais;
- logs, bancos de dados e binários da versão em produção.

## Requisitos

- JDK 17 ou superior.

## Compilar

### Windows

Execute:

```bat
build.bat
```

### Linux/macOS

Execute:

```bash
./build.sh
```

O JAR será criado em `dist/GestaoToner-Academico.jar`.

## Executar

Após compilar:

```bash
java -jar dist/GestaoToner-Academico.jar
```

## Estrutura

```text
src/            código-fonte Java
dados-academico/ arquivos locais gerados em execução (não versionados)
config/         exemplo de configuração sem credenciais
docs/           exemplos e documentação complementar
assets/         ícones do projeto
manifest.txt    manifesto para geração do JAR
build.bat       compilação no Windows
build.sh        compilação no Linux/macOS
```

## Integração com Power Automate

A versão pública não possui URL, e-mail ou credencial pré-configurados. Além disso, enquanto `ACADEMIC_DEMO_MODE` estiver habilitado, os envios pelo Power Automate são **simulados** e nenhuma chamada de rede é realizada, mesmo que uma URL seja preenchida. O arquivo `config/email.properties.example` serve apenas como referência e utiliza valores fictícios.

## Observação acadêmica

A opção **Consulta Contador** utiliza valores simulados nesta versão para evitar qualquer tentativa de acesso à rede interna do ambiente onde a aplicação original é utilizada. A lógica principal da interface é mantida para fins de demonstração do MVP.

## Autor

Elieder — projeto acadêmico de software.


---

## Deploy acadêmico no Render

Este repositório é uma cópia específica para disponibilização do MVP acadêmico pela internet.
O código Java Swing permanece em modo de demonstração, sem credenciais, dados operacionais reais
ou acesso à rede interna.

A camada de deploy utiliza Docker, Java 17, Xvfb, Openbox, x11vnc, noVNC/Websockify e Render.

### Fluxo de acesso

```text
Navegador
   ↓
Render (HTTPS)
   ↓
noVNC / Websockify
   ↓
x11vnc
   ↓
Xvfb + Openbox
   ↓
Gestão de Toner - MVP Acadêmico (Java Swing)
```

### Persistência

No plano gratuito do Render, o armazenamento local é efêmero. Os dados criados durante a
demonstração podem ser reinicializados após um novo deploy ou reinício do serviço. Isso é
adequado para esta versão pública de demonstração.

### Execução local via Docker

```bash
docker build -t gestao-toner-academico .
docker run --rm -p 10000:10000 -e PORT=10000 gestao-toner-academico
```

Depois, abra `http://localhost:10000`.
