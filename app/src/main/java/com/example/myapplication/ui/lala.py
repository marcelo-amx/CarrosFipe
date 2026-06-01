import json

file_path = "C:/Users/User/AndroidStudioProjects/CarrosFIpe/app/src/main/res/raw/filtrada.json"

with open(file_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

# Dicionário de mapeamento para normalização
mapping = {
    "HONERT": "Hornet",
    "COROLA": "Corolla",
    "FILDER": "Fielder",
    "ÔNIX": "Onix",
    "ONIX": "Onix",
    "CRUZER": "Cruze",
    "DREAN": "Dream",
    "TINTAN": "Titan",
    "HONER": "Hornet",
    "TITANIUN": "Titanium",
    "PICA-UP": "Pick-up",
    "CONSTALLATION": "Constellation",
    "M.POLO": "Marcopolo",
    "BENS": "Benz",
    "VITO": "Vito",
    "C-200, CLA-200": "C-200 / CLA-200",
    "A3, S3": "A3 / S3",
    "BW246": "B 200 (W246)",
    "B180": "B 180",
    "B200": "B 200",
    "B250": "B 250",
}

def normalize_model(model):
    if not model:
        return model

    m = model.upper()

    # Correções específicas de termos errados
    m = m.replace("HONERT", "Hornet")
    m = m.replace("DREAN", "Dream")
    m = m.replace("COROLA", "Corolla")
    m = m.replace("TITANIUN", "Titanium")
    m = m.replace("CRUZER", "Cruze")
    m = m.replace("ÔNIX", "Onix")
    m = m.replace("FILDER", "Fielder")
    m = m.replace("VECTRA", "Vectra")
    m = m.replace("PICA-UP", "Pick-up")
    m = m.replace("S-10", "S10")

    # Normalização de Motos Honda
    if "BIZ" in m:
        m = m.replace("BIZ 110I", "Biz 110i").replace("BIZ 125 EX", "Biz 125 EX").replace("BIZ 100", "Biz 100")
    if "CG" in m:
        m = m.replace("150 TITAN", "150 Titan").replace("160 TITAN", "160 Titan").replace("125 TITAN", "125 Titan")

    # Normalização de Caminhões (Remover hífens desnecessários ou padronizar)
    if "CONSTELLATION" in m or "CONSTALLATION" in m:
        m = m.replace("CONSTALLATION", "Constellation").replace("CONSTELLATION", "Constellation")

    # Ajuste de Capitalização (Apenas a primeira letra ou padrão FIPE)
    # Como são muitos casos, aplicamos um título inteligente
    return m.strip().title().replace("Bmw", "BMW").replace("Vw", "VW").replace("Gm", "GM").replace("Fiesta-Imp", "Fiesta Imp.")

for item in data:
    original = item.get("modelo_atual", "")
    # Aplicação das correções de mapeamento conhecidas
    for key, val in mapping.items():
        if key in original.upper():
            original = original.upper().replace(key, val)

    # Limpeza de sufixos de geração/marketing que não constam na FIPE
    for suffix in [" G1", " G2", " G3", " G4", " G5", " G6", " G7", " (NOVO)", " TODOS", " ANTIGO", " ATUAIS"]:
        original = original.replace(suffix, "").replace(suffix.lower(), "")

    item["modelo_atual"] = original.strip()

# Salvando de volta no arquivo
with open(file_path, 'w', encoding='utf-8') as f:
    json.dump(data, f, indent=2, ensure_ascii=False)