var exec = require('cordova/exec');

/**
 * Ouvre la caméra. success reçoit :
 * - un tableau de chemins à la fin de la session (OK ou annulation) ;
 * - sur Android, un objet { type: 'edit', path, index } quand l'utilisateur veut annoter une photo :
 *   la caméra reste affichée jusqu'à pauseCamera (éditeur prêt), puis en pause jusqu'à resumeCamera.
 * arg0[2] = { deferClose: true } (Android) : à la fin de la session la caméra reste affichée jusqu'à closeCamera.
 */
exports.takePictures = function (arg0, success, error) {
    exec(success, error, 'ScoplanCamera', 'takePictures', arg0);
};

/** Masque la caméra pour révéler l'éditeur web, ouvert derrière elle. */
exports.pauseCamera = function (success, error) {
    exec(success, error, 'ScoplanCamera', 'pauseCamera', []);
};

/** Réaffiche la caméra après l'édition : { index, path } (photo annotée), { index, removed: true } ou { index }. */
exports.resumeCamera = function (options, success, error) {
    exec(success, error, 'ScoplanCamera', 'resumeCamera', [options]);
};

/** Ferme la caméra laissée ouverte par deferClose. */
exports.closeCamera = function (success, error) {
    exec(success, error, 'ScoplanCamera', 'closeCamera', []);
};
