-- Honest point ledger: tape does not carry shot outcome or rally length.
ALTER TABLE match_points ALTER COLUMN rally_length DROP NOT NULL;
ALTER TABLE match_points ALTER COLUMN rally_length DROP DEFAULT;

-- Purge the retired hand-authored broadcast catalogue.
DELETE FROM matches
WHERE external_id IN (
    'wimbledon-2026-ms-sf-alcaraz-sinner',
    'wimbledon-2026-ms-sf-djokovic-medvedev',
    'wimbledon-2026-ms-qf-zverev-ruud',
    'wimbledon-2026-ws-qf-swiatek-gauff',
    'wimbledon-2026-ws-qf-sabalenka-rybakina',
    'rg-2026-ms-qf-alcaraz-zverev',
    'rg-2026-ms-sf-sinner-djokovic',
    'rg-2026-ws-r16-gauff-keys',
    'uso-2026-ms-r32-sinner-fonseca',
    'uso-2026-ms-r32-medvedev-rublev',
    'uso-2026-ws-r16-swiatek-paolini',
    'uso-2026-ms-r64-ruud-tsitsipas'
);
