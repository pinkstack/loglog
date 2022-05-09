with (import <nixpkgs> {});

mkShell {
  buildInputs = [
    ansible
    influxdb
    jdk17_headless
    redis
    ruby
    sbt
  ];
  shellHook = ''
    export LOGLOG_HOME=`pwd`
    export ANSIBLE_HOST_KEY_CHECKING=False
    export ANSIBLE_INVENTORY=inventory.yaml
  '';
}
