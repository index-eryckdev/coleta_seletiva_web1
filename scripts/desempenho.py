#!/usr/bin/env python3
"""Mede consultas HTTP com 1.000 solicitações em um banco descartável."""
import argparse
import json
import math
import statistics
import time
from urllib.parse import urlencode
from urllib.request import Request, urlopen

ADMIN = {"X-Ator-Tipo": "ADMINISTRADOR"}
MORADOR = {"X-Ator-Tipo": "MORADOR", "X-Ator-Id": "1"}
OPERADOR = {"X-Ator-Tipo": "OPERADOR", "X-Ator-Id": "1"}
FUTURO = "2099-01-01T12:00:00-03:00"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--url", default="http://127.0.0.1:8080")
    parser.add_argument("--amostras", type=int, default=100)
    args = parser.parse_args()
    if args.amostras < 5:
        parser.error("Informe ao menos cinco amostras.")

    def pedir(caminho, ator=None, corpo=None, metodo=None, **consulta):
        url = args.url.rstrip("/") + "/api" + caminho
        if consulta:
            url += "?" + urlencode(consulta)
        headers = {"Accept": "application/json", **(ator or {})}
        data = None
        if corpo is not None:
            headers["Content-Type"] = "application/json"
            data = json.dumps(corpo).encode()
        with urlopen(Request(url, data=data, headers=headers, method=metodo), timeout=15) as resposta:
            return json.load(resposta)

    if pedir("/solicitacoes", ADMIN, size=1)["totalElementos"] != 0:
        parser.error("Use um banco descartável sem solicitações para obter uma carga de exatamente 1.000.")
    endereco = pedir("/moradores/1/enderecos", MORADOR)["conteudo"][0]["id"]
    exemplo = None
    for i in range(1000):
        ponto = 1 if i % 2 == 0 else 3
        resposta = pedir("/solicitacoes", MORADOR, {
            "pontoId": ponto,
            "enderecoId": endereco,
            "itens": [{"materialId": 1, "quantidadeEstimada": 2}, {"materialId": 3, "quantidadeEstimada": 1}],
        })["solicitacao"]
        if i < 20 and ponto == 1:
            id = resposta["id"]
            pedir(f"/solicitacoes/{id}/agendar", OPERADOR, {"versao": 0, "dataAgendamento": FUTURO}, "PATCH")
            pedir(f"/solicitacoes/{id}/iniciar", OPERADOR, {"versao": 1}, "PATCH")
            pedir(f"/solicitacoes/{id}/concluir", OPERADOR, {"versao": 2, "itens": [
                {"materialId": 1, "quantidadeEfetiva": 1.25}, {"materialId": 3, "quantidadeEfetiva": 0},
            ]}, "PATCH")
            exemplo = resposta
    assert pedir("/solicitacoes", ADMIN, size=1)["totalElementos"] == 1000

    consultas = [
        lambda: pedir("/solicitacoes", ADMIN, size=20),
        lambda: pedir("/solicitacoes", ADMIN, pontoId=1, situacao="SOLICITADA", page=4, size=20),
        lambda: pedir(f"/solicitacoes/{exemplo['id']}", MORADOR),
        lambda: pedir("/indicadores", ADMIN),
        lambda: pedir("/pontos", materialId=3, municipio="Aracaju", size=20),
    ]
    for consulta in consultas:
        consulta()
    tempos = []
    for i in range(args.amostras):
        inicio = time.perf_counter()
        consultas[i % len(consultas)]()
        tempos.append((time.perf_counter() - inicio) * 1000)
    ordenados = sorted(tempos)
    percentil95 = ordenados[math.ceil(len(ordenados) * .95) - 1]
    print(json.dumps({
        "solicitacoes": 1000,
        "consultas": len(tempos),
        "mediana_ms": round(statistics.median(tempos), 2),
        "p95_ms": round(percentil95, 2),
        "maximo_ms": round(max(tempos), 2),
        "ate_2000_ms": sum(t <= 2000 for t in tempos),
        "id_verificacao_persistencia": exemplo["id"],
        "protocolo_verificacao_persistencia": exemplo["protocolo"],
    }, indent=2))
    if percentil95 > 2000:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
