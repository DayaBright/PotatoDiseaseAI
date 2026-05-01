from tflite_support.metadata_writers import image_classifier
from tflite_support.metadata_writers import writer_utils

# 1. Definir los nombres de archivos directamente como texto (sin OUTPUT_DIR)
# Cambia 'potato_classifier (2).tflite' al nombre exacto de tu modelo descargado
MODEL_PATH = 'potato_classifier.tflite' 
METADATA_MODEL_PATH = 'potato_classifier_with_metadata.tflite'
LABELS_PATH = 'labels.txt'

# 2. Las clases exactas en el orden de tu dataset
classes = ['early_blight', 'healthy', 'late_blight', 'leafroll_virus', 'mosaic_virus', 'nematode', 'pest'] 

# Crear el archivo labels.txt
with open(LABELS_PATH, 'w') as f:
    f.write('\n'.join(classes))

# 3. Configurar los parámetros de normalización para MobileNetV2 ([-1, 1])
_INPUT_NORM_MEAN = 127.5
_INPUT_NORM_STD = 127.5

# 4. Crear el empaquetador de metadatos
ImageClassifierWriter = image_classifier.MetadataWriter
writer = ImageClassifierWriter.create_for_inference(
    writer_utils.load_file(MODEL_PATH),
    [_INPUT_NORM_MEAN], 
    [_INPUT_NORM_STD], 
    [LABELS_PATH]
)

# 5. Generar el nuevo modelo con los metadatos integrados
writer_utils.save_file(writer.populate(), METADATA_MODEL_PATH)

print(f"✅ ¡Metadatos integrados correctamente!")
print(f"Modelo con metadatos guardado en: {METADATA_MODEL_PATH}")