**(H) - Conduzir alguns testes manuais (via GUI) usando os critérios vistos em sala de aula**



**==================================================**

**FUNCIONALIDADE: Adicionar artigo (New entry - Article)**

**TELA: Biblioteca > New entry > Article**

**TESTADOR: João Lucas Gomes Pelegrino**

**DATA: 22/12/2025**

**CRITÉRIOS: PCE (Particionamento em Classes de Equivalência) e AVL (Análise de Valor Limite)**



**PRÉ-CONDIÇÕES:**

* **JabRef aberto**
* **Biblioteca vazia/criada**
* **Nenhuma entrada selecionada**



**==================================================**

**FUNCIONALIDADE: CRIAR ARTIGO**

**==================================================**



**CASO DE TESTE CT01 - Criar artigo com dados válidos**

**Classe: VÁLIDA (entrada "normal")**



**Passos:
1.** Criar a pasta clicando em "New empty library"

**2.** Clicar em "+" que representa "Add entry" para criar um novo artigo.

**3.** Preencher Author = "Pelegrino, João Lucas".

**4.** Preencher Title = "Teste 1".

**5.** Preencher Journal = "Caixa Preta".

**6.** Preencher Year = "2024".

**7.** Preencher Citation key = "Pelegrino2024".

**8.** Clicar em "X" para sair da célula.



**Resultado ESPERADO:**

* Artigo aparece na tabela principal
* Nenhum aviso de erro (campos em branco ou ícones de alerta)



**Resultado OBTIDO:**

* O artigo foi criado corretamente
* Apareceu na lista principal



**Status: Aprovado**

**Observações:**

* Sem observações nesse teste

**--------------------------------------------------**

**CASO DE TESTE CT02 - Citation key vazio**

**Classe: INVÁLIDA (campo importante vazio ou formatado da forma errada)**



**Passos:**

**1.** Criar novo Article.

**2.** Preencher todos os campos obrigatórios da mesma forma, EXCETO "Citation key" ou colocar em "Citation key" alguma palavra com espaçamento ex. "Teste Endo".

**3.** E fechar o artigo em "X"(salva ele na página principal).



**Resultado ESPERADO:**

* Campo "Citation key" fica marcado com alerta ou não permite a criação desse artigo



**Resultado OBTIDO:**

* Foi possível criar o artigo, mas ao não colocar a informação aparece um alerta em formato de placa de alerta e passando o mouse em cima diz: "empty citation key" 



**Status: Aprovado**

**Observações:**

* Achei interessante esse formato de alerta, pois no lugar de não deixar criar o artigo, mesmo sendo um campo importante para a questão de organização e busca, ele permite a criação e apenas da um alerta



**--------------------------------------------------**

**CASO DE TESTE CT03 - Year com texto em vez de número**

**Classe: INVÁLIDA (tipo errado)**



**Passos:**

**1.** Criar novo Artigo.

**2.** Preencher Year = "abcd".

**3.** Preencher o restante normalmente.

**4.** Fechar o Artigo(o mesmo que salvar).



**Resultado ESPERADO:**

* Campo Year deve recusar valor não numérico OU mostrar mensagem de erro E não permitir a criação do artigo



**Resultado OBTIDO:**

* Foi possível criar o artigo, mas ao colocar letras na célula ano, aparece um alerta em formato de placa de alerta e passando o mouse em cima diz: "shuold contain a four digit numbers"



**Status: Reprovado**

**Observações:**

* A informação do ano é uma informação relevante e indica um erro e não a falta de vontade de colocar a informação correta como no caso do "Citation Key", logo não deveria permitir a criação do artigo



**--------------------------------------------------**

**CASO DE TESTE CT04 - Year no limite (AVL)**

**Classe: LIMITE (Análise de Valor Limite)**



**Passos:**

**1.** Criar novo Article.

**2.** Preencher Year = "0".

**3.** Preencher o restante normalmente.

**4.** Tentar salvar.



**Resultado ESPERADO:**

* Permitir a criação do artigo, mas alertar possível erro



**Resultado OBTIDO:**

* O artigo pode ser criado e ao digitar o valor "0" ele sinaliza com a placa no formato de alerta dizendo: "shuold contain a four digit numbers"



**Status: Aprovado**

**Observações:**

* Apenas alerta que pode ser um erro



**--------------------------------------------------**

**CASO DE TESTE CT05 - Year muito grande (AVL)**

**Classe: LIMITE SUPERIOR**



**Passos:**

**1.** Criar novo Article.

**2.** Preencher Year = "9999".

**3.** Preencher o restante normalmente.

**4.** Tentar salvar.



**Resultado ESPERADO:**

* Não permitir o salvamento do arquivo por ser uma data no futuro e só é possível er artigos criados no passado ou presente E sinalizar o erro alertando o usuário



**Resultado OBTIDO:**

* Ele permite a criação e não sinaliza pro usuário o erro no valor colocado de "9999"



**Status: Reprovado**

**Observações:**

* Aqui temos um erro claro de digitação por isso não deveria permitir a criação



**==================================================**

**FUNCIONALIDADE: CRIAR GRUPO**

**==================================================**


**CASO DE TESTE CT01 - Criar grupo com nome válido**

**Classe: VÁLIDA**



**Passos:**

**1.** Clicar em "Add group" na lateral esquerda da home.

**2.** Clicar em "Add group" no canto inferior esquerdo.

**3.** No campo Name, preencher “Grupo Caixa Preta”.

**4.** Manter “Collect by = Explicit selection” (padrão).

**5.** Clicar em OK.



**Resultado ESPERADO:**

* Grupo aparece na lista de grupos
* Nenhum alerta visual
* Botão OK habilitado



**Resultado OBTIDO:**

* O botão OK foi habilitado, não apareceu nenhum alerta visual e aparece que foi adicionado esse grupo a lista de grupos



**Status: Aprovado**

**Observações:**

* Sem comentários, tudo correu como o esperado



**--------------------------------------------------**

**CASO DE TESTE CT02 - Criar grupo com nome vazio ou só espaços**

**Classe: INVÁLIDA**



**Passos:**

**1.** Clicar em "Add group" na lateral esquerda da home.

**2.** Clicar em "Add group" no canto inferior esquerdo.

**3.** Deixar o campo Name vazio OU preencher só com espaços.

**4.** Clicar em OK.



**Resultado ESPERADO:**

* Sistema não habilita o botão OK pra criação do novo grupo e aleta o usuário



**Resultado OBTIDO:**

* Resultado obtido foi exatamente como o esperado



**Status: Aprovado**

**Observações:**

* Sem comentários, tudo correu como o esperado



**--------------------------------------------------**

**CASO DE TESTE CT03 — Selecionar “Searching for a keyword” e não preencher os campos**

**Classe: INVÁLIDA (opção exige informações obrigatórias)**



**Passos:**

**1.** Clicar em "Add group" na lateral esquerda da home.

**2.** Clicar em "Add group" no canto inferior esquerdo.

**3.** Alterar “Collect by” para Searching for a keyword.

**4.** Deixar Field e Keyword vazios.

**5.** Clicar em OK.



**Resultado ESPERADO:**

* Botão OK deve ficar desabilitado
* Sistema deve indicar campos obrigatórios não preenchidos



**Resultado OBTIDO:**

* O botão OK fica desabilitado e ao lado esquerdo do campo o sistema alerta o usuário com um ícone de "!" para os campos obrigatórios não preenchidos



**Status: Aprovado**

**Observações:** campos obrigatórios ficam com ícone preto, mensagem de erro aparece ao passar mouse e a interface mostra de forma clara quais campos faltam



