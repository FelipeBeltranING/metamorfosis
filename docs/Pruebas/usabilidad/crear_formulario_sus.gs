/**
 * Crea en Google Forms el cuestionario SUS de Metamorfosis (sección 6.2 de la entrega final).
 *
 * Uso:
 *   1. Abrir https://script.google.com, «Nuevo proyecto», borrar el código de ejemplo y pegar este archivo.
 *   2. Guardar, elegir la función crearFormularioSUS y tocar «Ejecutar». La primera vez Google pide
 *      autorizar el acceso a Forms y a Hojas de cálculo de tu cuenta.
 *   3. En «Registro de ejecución» salen el enlace para enviar a las personas, el de edición y el de la hoja
 *      de respuestas.
 *
 * Las 10 frases van en el mismo orden que SUS_guia_y_cuestionario.md, porque calcular_sus.py resta distinto
 * en las impares y en las pares. No cambiar el orden ni el sentido de las frases.
 */
function crearFormularioSUS() {
  var form = FormApp.create('Prueba de usabilidad — Metamorfosis');
  form.setDescription(
    'Gracias por ayudarnos. Evaluamos la aplicación, no a ti, y tus respuestas son anónimas.\n' +
    'Responde cada frase de 1 (totalmente en desacuerdo) a 5 (totalmente de acuerdo), sin pensarlo mucho.'
  );
  form.setCollectEmail(false);
  form.setLimitOneResponsePerUser(false);
  form.setProgressBar(false);

  form.addMultipleChoiceItem()
    .setTitle('¿En qué dispositivo usaste la aplicación?')
    .setChoiceValues(['Celular', 'Computador'])
    .setRequired(true);

  var frases = [
    'Creo que me gustaría usar esta aplicación con frecuencia.',
    'Encontré la aplicación innecesariamente compleja.',
    'Pienso que la aplicación es fácil de usar.',
    'Creo que necesitaría la ayuda de una persona con conocimientos técnicos para poder usarla.',
    'Las funciones de la aplicación están bien integradas.',
    'Pienso que la aplicación tiene demasiadas inconsistencias.',
    'Imagino que la mayoría de las personas aprendería a usarla muy rápido.',
    'Encontré la aplicación muy difícil de usar.',
    'Me sentí muy seguro(a) usando la aplicación.',
    'Necesité aprender muchas cosas antes de poder usar la aplicación.'
  ];
  frases.forEach(function (frase, i) {
    form.addScaleItem()
      .setTitle((i + 1) + '. ' + frase)
      .setBounds(1, 5)
      .setLabels('Totalmente en desacuerdo', 'Totalmente de acuerdo')
      .setRequired(true);
  });

  form.addParagraphTextItem()
    .setTitle('¿Algo que te haya costado o que mejorarías? (opcional)');

  var hoja = SpreadsheetApp.create('Respuestas — Prueba de usabilidad Metamorfosis');
  form.setDestination(FormApp.DestinationType.SPREADSHEET, hoja.getId());

  Logger.log('Enlace para enviar a las personas: ' + form.getPublishedUrl());
  Logger.log('Enlace para editar el formulario: ' + form.getEditUrl());
  Logger.log('Hoja de respuestas: ' + hoja.getUrl());
}
