with (import <nixpkgs> {});

mkShell {
  buildInputs = [
    ansible
    influxdb
    jdk17_headless
    ruby
    sbt
    flyctl
  ];
  shellHook = ''
    export LOGLOG_HOME=`pwd`
    export ANSIBLE_HOST_KEY_CHECKING=False
    export ANSIBLE_INVENTORY=inventory.yaml
  '';
}
