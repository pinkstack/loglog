with (import <nixpkgs> {});

mkShell {
  buildInputs = [
    ansible
    influxdb
    jdk17_headless
    redis
    ruby
    sbt
    nodejs
    yarn
  ];
  shellHook = ''
    export LOGLOG_HOME=`pwd`
    export ANSIBLE_HOST_KEY_CHECKING=False
    export ANSIBLE_INVENTORY=inventory.yaml
    export PATH="$PWD/node_modules/.bin/:$PATH"
    yarn install
  '';
}
