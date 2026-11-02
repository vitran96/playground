# Apache HTTP Server with mod_auth_openidc
# Used by: Architectures 1, 2, 3 (gw-3-int, gw-3-ext), and 4 (gw-4-int, gw-4-ext)

FROM docker.io/library/debian:bookworm-slim

RUN apt-get update \
 && apt-get install -y --no-install-recommends apache2 libapache2-mod-auth-openidc ca-certificates \
 && rm -rf /var/lib/apt/lists/* \
 && a2enmod proxy proxy_http headers alias auth_openidc \
 && a2dissite 000-default \
 && echo 'ServerName gateway' > /etc/apache2/conf-enabled/servername.conf

COPY conf/common.conf /etc/apache2/gateway/common.conf

EXPOSE 80
CMD ["apache2ctl", "-D", "FOREGROUND"]
