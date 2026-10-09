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

## 8. Atualização 7: interface e atmosfera
- **HUD:** só o botão "Menu" no canto superior esquerdo; ele abre e fecha as opções e os controles. Duplo toque ou F11 esconde tudo, inclusive o Menu.
- **Nuvens:** textura pré-calculada no lugar de ruído por pixel (muito menos custo por fragmento) e mais sprites, para dar densidade interna.
- **Atmosfera (rota real):** céu azulado e relâmpagos intranuvem silenciosos (clarões e tremulação, sem trovão), conforme os relatos do dia.
- **Escala:** objetos em 1:1. A cidade modelada tem 840×504 m, menor que a área urbana real. A expansão com a malha real (dados do OpenStreetMap) fica como trabalho futuro.

## 9. Incêndios e vazamentos
Uma zona circular (raio de 110 a 170 m, perto da trajetória) é sorteada com semente. Nela, cada casa de madeira tem 10 a 25% de chance de foco de incêndio e de água. Nas demais casas, só 10% de chance de rompimento de tubulação, com jato de água para cima. Postes e carros só pegam fogo dentro da zona. No máximo 8 jatos ficam ativos ao mesmo tempo.

## 10. Visual unificado
A estética das nuvens (tons azul-acinzentados, transições suaves) foi estendida à cena toda: luz ambiente e solar mais frias, névoa atmosférica azulada que aumenta com a tempestade, variação de cor por ruído com sombras azuladas nas superfícies e vinheta suave. Para mais nitidez: antialiasing (MSAA), resolução inicial maior (adaptativa ao FPS) e filtragem anisotrópica nas texturas.
