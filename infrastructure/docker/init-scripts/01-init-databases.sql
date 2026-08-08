CREATE DATABASE tennisly_users;
CREATE DATABASE tennisly_auth;
CREATE DATABASE tennisly_matches;
CREATE DATABASE tennisly_tennis_data;
CREATE DATABASE tennisly_replay;
CREATE DATABASE tennisly_analytics;
CREATE DATABASE tennisly_notifications;
CREATE DATABASE tennisly_billing;

GRANT ALL PRIVILEGES ON DATABASE tennisly_users TO tennisly;
GRANT ALL PRIVILEGES ON DATABASE tennisly_auth TO tennisly;
GRANT ALL PRIVILEGES ON DATABASE tennisly_matches TO tennisly;
GRANT ALL PRIVILEGES ON DATABASE tennisly_tennis_data TO tennisly;
GRANT ALL PRIVILEGES ON DATABASE tennisly_replay TO tennisly;
GRANT ALL PRIVILEGES ON DATABASE tennisly_analytics TO tennisly;
GRANT ALL PRIVILEGES ON DATABASE tennisly_notifications TO tennisly;
GRANT ALL PRIVILEGES ON DATABASE tennisly_billing TO tennisly;

\c tennisly_users
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c tennisly_auth
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c tennisly_matches
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c tennisly_tennis_data
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c tennisly_replay
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c tennisly_analytics
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c tennisly_notifications
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c tennisly_billing
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

