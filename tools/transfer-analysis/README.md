# Mapeamento de transferências

O módulo está implementado em `../NegotiationRules.java` e `../TransferNegotiation.java`.
`../build-transfers.ps1` gera um executável separado e executa testes com dados fictícios.

Entradas confirmadas por inspeção do bytecode:

- `a.iA`: mercado; `FN` = Comprar, `MK` = Fazer oferta; `sG()` atualiza disponibilidade; `sA()` abre oferta, preservando verificação de registro `c.a.vL()`.
- `a.cz`: diálogo de oferta; `oL()` avalia valor e exige salário; o booleano `CZ` diferencia empréstimos.
- `a.jm`: ofertas recebidas; `Nw` é valor, `Nu` comprador, `uz` jogador, `Nx` empréstimo; `qX()` efetiva aceite; `Nv` sinaliza resultado ao chamador.
- `best.F.fg()` clube; `fk()` valor de mercado; `fl()` preço pedido; `ft()` à venda; `fi()` força; `fj()` salário; `ae(int)` atualiza salário.
- `best.F.fR()` dias restantes de contrato; `a(long, boolean)` define prazo em dias a partir da data do jogo quando boolean=true.
- `best.F.a(best.ah,int,boolean,boolean,boolean)` transfere, registra histórico e movimenta finanças dos clubes humanos; redefine contrato, portanto prazo/salário acordados precisam ser aplicados depois.
- `best.ah.getReputacao()` reputação; `kb()` caixa; `kc()` elenco; `jZ()` controle humano; `lk()` identificador.

Comportamento implementado:

- Mercado `a.iA`: Comprar quando à venda; Fazer Proposta nos demais casos. Mantém a verificação de registro para ofertas fora da lista.
- Fluxos `a.cz` e `a.jm`: negociação de compra e de ofertas recebidas; os caminhos de empréstimo existentes são preservados.
- Quatro rodadas por etapa. Cada lado possui alvo, limite e paciência: contrapropostas cedem progressivamente quando a outra parte melhora a oferta, mas propostas fracas, repetidas ou abusivas podem encerrar a conversa.
- Bloqueio de 14 dias pelo calendário da carreira, armazenado em `mods/negociacoes.properties` e isolado pelo nome estável do save.
- Método nativo efetiva mudança de elenco e histórico; caixa dos clubes da IA é ajustado uma vez, pois o método original só contabiliza os humanos.
- Satisfação é um modelo novo estimado por utilização na temporada, salário relativo, prazo contratual e lista de venda. Utilização usa partidas, não minutos. Potencial é estimado por idade e força. Reputação nativa de 1–5 é normalizada para 0–100.

Limitações explícitas desta versão:

- Rivalidade, classificação exata na temporada e qualificação continental ainda não estão conectadas ao contexto; divisão é usada como sinal de oportunidade esportiva.
- Negociação contratual do comprador IA usa uma oferta calculada e contrato de um ano; o usuário negocia salário/prazo quando ele é o comprador.
- O teste de integração usa classes reais do jogo com dados sintéticos, sem carregar uma carreira real. Validação manual numa carreira continua necessária.
- Bônus, cláusulas e empréstimos não foram ampliados nesta etapa.

Testes: rejeição/contraproposta/aceite, influência de salário e reputação, orçamento, limite de rodadas, transferência nativa, débito/crédito único, prazo e salário, bloqueio persistente por save e expiração após 14 dias.

Patrocínio: o contrato usa `save:<nome do save>#club:<id>` em `mods/patrocinios.properties`. O texto da partida/rodada deixa de compor a identidade, e uma carreira nova não herda o contrato de outra. Nenhum campo é adicionado às classes serializadas do jogo, preservando saves antigos.
