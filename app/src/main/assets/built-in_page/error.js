const textUrl = document.querySelector('.url-text');
const errorText = document.querySelector('.error-text');
const offlineBanner = document.querySelector('.offline-banner');
const btnReload = document.querySelector('.btn-reload');

textUrl.textContent = location.href;
textUrl.href = location.href;

errorText.textContent = EVO_ERROR.name + " (" + EVO_ERROR.code + ")";

if (navigator.onLine) {
    offlineBanner.style.display = 'none';
} else {
    offlineBanner.style.display = 'flex';
}

window.addEventListener('online', () => {
    offlineBanner.style.display = 'none';
});

window.addEventListener('offline', () => {
    offlineBanner.style.display = 'flex';
});

btnReload.addEventListener('click', () => {
    location.reload();
});
