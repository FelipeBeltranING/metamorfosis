-- HU20 (rediseñada): la mascota se reemplaza por un avatar personalizable. Cada persona arma su
-- avatar pieza por pieza (piel, peinado, color de pelo, ojos, boca y un accesorio opcional) y se
-- muestra asomado en su perfil, así otros la reconocen sin ver su foto. Los valores son los del
-- estilo «Big Smile» de DiceBear y son los mismos de CatalogoAvatar en el backend.
alter table usuario drop constraint ck_usuario_mascota;
alter table usuario drop column mascota;

alter table usuario
    add column avatar_piel       varchar(6)  not null default 'f5d7b1',
    add column avatar_peinado    varchar(20) not null default 'shortHair',
    add column avatar_color_pelo varchar(6)  not null default '71472d',
    add column avatar_ojos       varchar(12) not null default 'cheery',
    add column avatar_boca       varchar(14) not null default 'openedSmile',
    add column avatar_accesorio  varchar(16);

alter table usuario
    add constraint ck_usuario_avatar_piel check (avatar_piel in (
        'ffe4c0', 'f5d7b1', 'efcc9f', 'e2ba87', 'c99c62', 'a47539', '8c5a2b', '643d19')),
    add constraint ck_usuario_avatar_peinado check (avatar_peinado in (
        'shortHair', 'mohawk', 'wavyBob', 'bowlCutHair', 'curlyBob', 'straightHair', 'braids',
        'shavedHead', 'bunHair', 'froBun', 'bangs', 'halfShavedHead', 'curlyShortHair')),
    add constraint ck_usuario_avatar_color_pelo check (avatar_color_pelo in (
        '220f00', '3a1a00', '71472d', 'e2ba87', '605de4', '238d80', 'd56c0c', 'e9b729')),
    add constraint ck_usuario_avatar_ojos check (avatar_ojos in (
        'cheery', 'normal', 'confused', 'starstruck', 'winking', 'sleepy', 'sad', 'angry')),
    add constraint ck_usuario_avatar_boca check (avatar_boca in (
        'openedSmile', 'unimpressed', 'gapSmile', 'openSad', 'teethSmile', 'awkwardSmile', 'braces', 'kawaii')),
    add constraint ck_usuario_avatar_accesorio check (avatar_accesorio is null or avatar_accesorio in (
        'catEars', 'glasses', 'sailormoonCrown', 'clownNose', 'sleepMask', 'sunglasses', 'faceMask', 'mustache'));

-- La auditoría pasa a registrar CAMBIAR_AVATAR. Los registros viejos con CAMBIAR_MASCOTA se quedan
-- como están: log_actividad es de solo escritura (V4) y el historial no se reescribe.
