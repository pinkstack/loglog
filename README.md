# logstream

This is a simple [ZIO] based application that collects concurrent online viewership metrics
from [Slovenian national broadcaster][rtvslo] and its [RTV 356 online services][rtv-356].

## Experimentation with networking

To demonstrate the services resiliancy to networking exceptions; the following example can be used to inject
networking "delay" into "gateway" nginx proxy container. This will cause the service to fail and yet proceed to operate.

```bash
./bin/loglog-dev.sh exec gateway tc qdisc add dev eth0 root netem delay 10s
```

To remote the delay from the container via `tc` you can use the following set of commands

```bash
./bin/loglog-dev.sh exec gateway tc qdisc del root dev eth0
```

## Authors

- [Oto Brglez](https://github.com/otobrglez)

[ZIO]: https://zio.dev

[rtvslo]: https://www.rtvslo.si

[rtv-356]: https://365.rtvslo.si
