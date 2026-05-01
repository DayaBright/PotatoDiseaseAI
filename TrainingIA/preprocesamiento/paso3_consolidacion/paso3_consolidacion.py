"""
PASO 3 — Consolidación del Dataset Unificado

Reglas aplicadas:
  - DS1 y DS2 excluidos completamente (subconjuntos de DS6 y DS7)
  - Fungi de DS4, DS5, DS6 → early_blight
  - Virus de DS5 → mosaic_virus
  - Virus de DS4 y DS6 → __virus_revisar__ (clasificación manual pendiente)
  - Duplicados entre datasets: conservar mayor resolución
    Prioridad: DS7 > DS6 > DS3 > DS4 > DS5
  - Duplicados entre clases: conservar en la clase correcta definida manualmente
  - Duplicados intra-clase: conservar primero alfabéticamente
  - En duplicados DS7 vs DS3: siempre conservar DS7

PREREQUISITO: Haber ejecutado paso2_auditoria_completa.py y tener
              auditoria_output/inventario_imagenes.csv generado.

"""

import csv
import shutil
import logging
import sys
from collections import defaultdict
from datetime import datetime
from pathlib import Path

from PIL import Image

# ════════════════════════════════════════════════════════════════════════════
# CONFIGURACIÓN
# ════════════════════════════════════════════════════════════════════════════

SCRIPT_DIR = Path(__file__).resolve().parent
PREPROC_DIR = SCRIPT_DIR.parent

PASO2_DIR = PREPROC_DIR / "paso2_analisisDuplicadas"
INVENTORY_CSV = PASO2_DIR / "auditoria_output" / "inventario_imagenes.csv"

OUTPUT_DIR = SCRIPT_DIR / "consolidacion_output"
REPORT_PATH = OUTPUT_DIR / "reporte_consolidacion.txt"
LOG_PATH = OUTPUT_DIR / "consolidacion.log"

# Datasets excluidos completamente
DATASETS_EXCLUIDOS = {"dataset1_warcoder", "dataset2_rgfhz"}

# Datasets activos y su prioridad para resolver duplicados
# Mayor número = mayor prioridad
DATASET_PRIORITY = {
    "dataset7_nirmalsankalana": 5,
    "dataset6_markkostantine":  4,
    "dataset3_nirmalsankalana": 3,
    "dataset4_NoDescription":   2,
    "dataset5_shahadhossin":    1,
}

# Reclasificación de __REVISAR__ por dataset
# formato: (dataset, clase_original) → clase_destino
RECLASIFICACION = {
    ("dataset4_NoDescription",  "__REVISAR__"): "early_blight",   # fungi DS4
    ("dataset5_shahadhossin",   "__REVISAR__"): "early_blight",   # fungi DS5 (incluye virus→mosaic ya separado abajo)
    ("dataset6_markkostantine", "__REVISAR__"): "early_blight",   # fungi DS6
}

# Virus que van a carpeta de revisión manual
# DS4 virus y DS6 virus originalmente estaban en carpeta "Virus" o "Fungal"
# Se identifican por original_folder
VIRUS_REVISAR_FOLDERS = {"virus", "fungal_diseases"}

# Clases finales válidas + carpeta de revisión
TARGET_CLASSES = [
    "late_blight", "early_blight", "leafroll_virus", "mosaic_virus",
    "bacterial_wilt", "nematode", "pest", "healthy", "__virus_revisar__"
]

# ─── LOGGING ────────────────────────────────────────────────────────────────
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[
        logging.FileHandler(
            LOG_PATH,
            encoding="utf-8"
        ),
        logging.StreamHandler(sys.stdout),
    ],
)
log = logging.getLogger(__name__)

# ════════════════════════════════════════════════════════════════════════════
# FUNCIONES AUXILIARES
# ════════════════════════════════════════════════════════════════════════════

def get_resolution_area(path: str) -> int:
    """Retorna área en píxeles (width * height). 0 si falla."""
    try:
        with Image.open(path) as img:
            w, h = img.size
            return w * h
    except Exception:
        return 0


def normalize_folder(folder_name: str) -> str:
    """Normaliza nombre de carpeta a minúsculas con guión bajo."""
    return folder_name.strip().lower().replace(" ", "_")


# ════════════════════════════════════════════════════════════════════════════
# PASO A — Cargar inventario y aplicar reglas de reclasificación
# ════════════════════════════════════════════════════════════════════════════

def load_and_reclassify(csv_path: Path) -> list:
    """
    Carga el CSV del inventario y aplica las reglas de reclasificación:
    - Excluye DS1 y DS2
    - Reclasifica fungi y virus según reglas definidas
    - Asigna clase_final a cada registro
    """
    if not csv_path.exists():
        raise FileNotFoundError(
            f"No se encontró {csv_path}\n"
            "Ejecuta primero paso2_auditoria_completa.py"
        )

    records = []
    excluidos = 0
    reclasificados = 0

    with open(csv_path, encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            ds   = row["dataset"]
            cls  = row["mapped_class"]
            orig = normalize_folder(row["original_folder"])

            # Excluir DS1 y DS2 completamente
            if ds in DATASETS_EXCLUIDOS:
                excluidos += 1
                continue

            # Determinar clase final
            clase_final = cls

            # Virus de DS4 y DS6 → __virus_revisar__
            if orig in VIRUS_REVISAR_FOLDERS and ds in {
                "dataset4_NoDescription", "dataset6_markkostantine"
            }:
                clase_final = "__virus_revisar__"
                reclasificados += 1

            # Virus de DS5 → mosaic_virus
            elif orig == "virus" and ds == "dataset5_shahadhossin":
                clase_final = "mosaic_virus"
                reclasificados += 1

            # Fungi de DS4, DS5, DS6 → early_blight
            elif cls == "__REVISAR__" and orig not in VIRUS_REVISAR_FOLDERS:
                key = (ds, cls)
                if key in RECLASIFICACION:
                    clase_final = RECLASIFICACION[key]
                    reclasificados += 1
                else:
                    clase_final = "__virus_revisar__"

            # __DESCONOCIDO__ → omitir
            elif cls == "__DESCONOCIDO__":
                excluidos += 1
                continue

            row["clase_final"] = clase_final
            records.append(row)

    log.info(f"Registros cargados  : {len(records)}")
    log.info(f"Excluidos (DS1/DS2) : {excluidos}")
    log.info(f"Reclasificados      : {reclasificados}")
    return records


# ════════════════════════════════════════════════════════════════════════════
# PASO B — Resolver duplicados
# ════════════════════════════════════════════════════════════════════════════

def resolve_duplicates(records: list) -> list:
    """
    Para cada grupo de imágenes con el mismo hash:
    1. Duplicados entre datasets: conservar mayor prioridad de dataset
       (DS7 > DS6 > DS3 > DS4 > DS5). En empate, mayor resolución.
    2. Duplicados entre clases: conservar la clase_final ya asignada
       (las reglas de reclasificación ya resolvieron esto).
    3. Duplicados intra-clase: conservar primero alfabéticamente.

    Retorna lista de registros únicos a copiar.
    """
    # Agrupar por hash
    hash_groups = defaultdict(list)
    for r in records:
        h = r.get("hash", "")
        if h and h != "ERROR":
            hash_groups[h].append(r)
        else:
            # Sin hash válido: incluir directamente
            hash_groups[r["path"]].append(r)

    kept = []
    removed_cross_ds    = 0
    removed_cross_cls   = 0
    removed_intra       = 0

    for h, group in hash_groups.items():
        if len(group) == 1:
            kept.append(group[0])
            continue

        datasets_in_group  = {r["dataset"]     for r in group}
        classes_in_group   = {r["clase_final"] for r in group}

        if len(datasets_in_group) > 1:
            # Duplicado entre datasets → conservar mayor prioridad
            group_sorted = sorted(
                group,
                key=lambda r: (
                    DATASET_PRIORITY.get(r["dataset"], 0),
                    int(r.get("width", 0)) * int(r.get("height", 0))
                ),
                reverse=True
            )
            kept.append(group_sorted[0])
            removed_cross_ds += len(group) - 1

        elif len(classes_in_group) > 1:
            # Duplicado entre clases → conservar la de mayor prioridad de dataset
            # (las reglas de reclasificación ya asignaron clase_final correcta)
            group_sorted = sorted(
                group,
                key=lambda r: DATASET_PRIORITY.get(r["dataset"], 0),
                reverse=True
            )
            kept.append(group_sorted[0])
            removed_cross_cls += len(group) - 1

        else:
            # Intra-clase / intra-dataset → conservar primero alfabéticamente
            group_sorted = sorted(group, key=lambda r: r["filename"])
            kept.append(group_sorted[0])
            removed_intra += len(group) - 1

    log.info(f"\n[Duplicados eliminados]")
    log.info(f"  Entre datasets  : {removed_cross_ds}")
    log.info(f"  Entre clases    : {removed_cross_cls}")
    log.info(f"  Intra-clase     : {removed_intra}")
    log.info(f"  Total eliminados: {removed_cross_ds + removed_cross_cls + removed_intra}")
    log.info(f"  Imágenes únicas : {len(kept)}")

    return kept


# ════════════════════════════════════════════════════════════════════════════
# PASO C — Crear estructura de carpetas y copiar imágenes
# ════════════════════════════════════════════════════════════════════════════

def build_consolidated_dataset(records: list):
    """
    Crea la estructura de carpetas consolidada y copia las imágenes.

    Estructura final:
    dataset_consolidado/
      ├── late_blight/
      ├── early_blight/
      ├── leafroll_virus/
      ├── mosaic_virus/
      ├── bacterial_wilt/
      ├── nematode/
      ├── pest/
      ├── healthy/
      └── __virus_revisar__/
    """
    # Crear carpetas
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    for cls in TARGET_CLASSES:
        (OUTPUT_DIR / cls).mkdir(exist_ok=True)

    log.info(f"\nCopiando imágenes a: {OUTPUT_DIR}")

    copied    = 0
    errors    = 0
    skipped   = 0
    class_counts = defaultdict(int)

    for r in records:
        clase = r["clase_final"]

        # Solo copiar clases válidas
        if clase not in TARGET_CLASSES:
            skipped += 1
            continue

        src  = Path(r["path"])
        dest = OUTPUT_DIR / clase / src.name

        # Si el nombre ya existe en destino (colisión de nombres distintos),
        # añadir sufijo con dataset de origen para evitar sobreescritura
        if dest.exists():
            stem = src.stem
            suffix = src.suffix
            dest = OUTPUT_DIR / clase / f"{stem}__{r['dataset']}{suffix}"

        try:
            shutil.copy2(src, dest)
            copied += 1
            class_counts[clase] += 1
        except Exception as e:
            log.warning(f"  Error copiando {src.name}: {e}")
            errors += 1

    log.info(f"\n[Copia completada]")
    log.info(f"  Copiadas : {copied}")
    log.info(f"  Errores  : {errors}")
    log.info(f"  Omitidas : {skipped}")

    return class_counts, copied, errors


# ════════════════════════════════════════════════════════════════════════════
# PASO D — Reporte de consolidación
# ════════════════════════════════════════════════════════════════════════════

def save_report(class_counts: dict, total_copied: int, total_errors: int):
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    total_entrenables = sum(
        class_counts.get(c, 0) for c in TARGET_CLASSES
        if c != "__virus_revisar__"
    )

    lines = []
    lines.append("=" * 60)
    lines.append("REPORTE DE CONSOLIDACIÓN — PASO 2")
    lines.append(f"Generado: {timestamp}")
    lines.append("=" * 60)
    lines.append(f"\nDatasets excluidos  : DS1 (subconjunto DS6), DS2 (subconjunto DS7)")
    lines.append(f"Datasets activos    : DS3, DS4, DS5, DS6, DS7")
    lines.append(f"Total copiadas      : {total_copied:,}")
    lines.append(f"Errores             : {total_errors}")
    lines.append(f"Total entrenables   : {total_entrenables:,} (sin __virus_revisar__)")
    lines.append("")
    lines.append("-" * 60)
    lines.append("CONTEO POR CLASE")
    lines.append("-" * 60)

    for cls in TARGET_CLASSES:
        n = class_counts.get(cls, 0)
        if cls == "__virus_revisar__":
            estado = "⚠ REVISIÓN MANUAL PENDIENTE"
        elif n >= 1000:
            estado = "✔ OK"
        elif n >= 400:
            estado = "⚠ Aceptable"
        else:
            estado = "✘ Crítico"
        bar = "█" * min(n // 100, 35)
        lines.append(f"  {estado:28} {cls:<22} {n:>5}  {bar}")

    lines.append("")
    lines.append("-" * 60)
    lines.append("PENDIENTES ANTES DEL PASO 3")
    lines.append("-" * 60)
    lines.append("  1. Revisar __virus_revisar__: clasificar cada imagen")
    lines.append("     en leafroll_virus o mosaic_virus (o excluir).")
    lines.append("  2. Revisar DS6 en dataset_consolidado/bacterial_wilt,")
    lines.append("     nematode y pest: identificar imágenes de planta")
    lines.append("     completa irrecuperables y eliminarlas manualmente.")
    lines.append("  3. Con eso resuelto → ejecutar Paso 3 (preprocesamiento).")
    lines.append("=" * 60)

    report_text = "\n".join(lines)

    # Guardar en archivo
    REPORT_PATH.write_text(report_text, encoding="utf-8")
    log.info(f"\n[OK] Reporte guardado: {REPORT_PATH}")

    # También imprimir en consola
    print("\n" + report_text)


# ════════════════════════════════════════════════════════════════════════════
# MAIN
# ════════════════════════════════════════════════════════════════════════════

def main():
    log.info("=" * 60)
    log.info("PASO 2 — Consolidación del Dataset Unificado")
    log.info("=" * 60)

    # A. Cargar y reclasificar
    log.info("\n[A] Cargando inventario y aplicando reclasificación...")
    records = load_and_reclassify(INVENTORY_CSV)

    # B. Resolver duplicados
    log.info("\n[B] Resolviendo duplicados...")
    unique_records = resolve_duplicates(records)

    # C. Copiar imágenes
    log.info("\n[C] Construyendo dataset consolidado...")
    class_counts, total_copied, total_errors = build_consolidated_dataset(unique_records)

    # D. Reporte
    log.info("\n[D] Generando reporte...")
    save_report(class_counts, total_copied, total_errors)

    log.info(f"\nDataset consolidado en: {OUTPUT_DIR.resolve()}")


if __name__ == "__main__":
    main()
