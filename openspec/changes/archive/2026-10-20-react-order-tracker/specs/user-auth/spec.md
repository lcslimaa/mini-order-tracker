# Spec Delta

## ADDED Requirements

### Requirement: Consulta da própria identidade autenticada
O sistema SHALL oferecer GET /api/auth/me, retornando 200 com id, name, email e role do usuário autenticado. SHALL obter a identidade do token validado e o perfil atual do banco, sem selecionar usuário por entrada do cliente ou retornar senha, hash ou segredos. A consulta não SHALL alterar dados ou permissões.

#### Scenario: Cliente consulta sua identidade
- **WHEN** um CLIENTE envia token válido para GET /api/auth/me
- **THEN** recebe apenas seus dados públicos de identidade e role CLIENTE

#### Scenario: Operador consulta sua identidade
- **WHEN** um OPERADOR envia token válido para GET /api/auth/me
- **THEN** recebe seus dados públicos de identidade e role OPERADOR

#### Scenario: Token ausente ou inválido
- **WHEN** a consulta recebe token ausente, expirado, adulterado ou referente a usuário inexistente
- **THEN** retorna 401 sem dados pessoais

#### Scenario: Tentativa de selecionar outro usuário ou perfil
- **WHEN** um usuário envia parâmetros adicionais indicando outra identidade ou perfil
- **THEN** esses parâmetros não alteram a identidade retornada, seu perfil ou suas permissões
