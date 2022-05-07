FROM nginx:alpine

RUN set -xe \
    && apk update && apk upgrade \
    && apk add --no-cache --update \
        bash \
        iproute2 \
        iptables \
        iputils\
        bind-tools \
        net-tools \
        iperf \
        tcpdump \
        nmap \
        netcat-openbsd \
        procps \
    && rm -rf /var/cache/apk/* /tmp/*