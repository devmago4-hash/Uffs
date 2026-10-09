# Tornado Lab — Rio Bonito do Iguaçu

Projeto Java 17 + Swing/AWT, sem bibliotecas externas.

## Termux
pkg install openjdk-17 unzip
unzip tornado-rbi-v2.zip
cd tornado-rbi-v2
javac -d out $(find src -name '*.java')
export DISPLAY=:0
termux-x11 :0 &
java -cp out com.tornado.TornadoApp

## Controles
- EF0 a EF5
- pico dentro da faixa
- raio do núcleo
- velocidade de deslocamento
- iniciar/pausar
- regenerar cenário
- arrastar para orbitar a câmera
- roda para zoom

A física é uma aproximação didática/experimental; não é CFD nem um modelo estrutural de engenharia.


## Termux — execução rápida
Com o Termux:X11 aberto:
```bash
cd ~/storage/downloads/tornado-rbi-v2-final
bash run-termux.sh
```
