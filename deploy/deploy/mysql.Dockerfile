FROM mysql:5.7.18

COPY database/hims-public.sql /docker-entrypoint-initdb.d/01-hims.sql
COPY backend/hims-backend-code-master/sql/appointment_phase1.sql /docker-entrypoint-initdb.d/02-appointment-phase1.sql
COPY backend/hims-backend-code-master/sql/appointment_phase2.sql /docker-entrypoint-initdb.d/03-appointment-phase2.sql
COPY backend/hims-backend-code-master/sql/appointment_phase6.sql /docker-entrypoint-initdb.d/04-appointment-phase6.sql
COPY deploy/mysql/04-create-app-user.sh /docker-entrypoint-initdb.d/05-create-app-user.sh
RUN chmod +x /docker-entrypoint-initdb.d/05-create-app-user.sh
