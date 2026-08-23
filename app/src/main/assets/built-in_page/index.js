const inputSearch = document.querySelector(".input-search");
const btnSearch = document.querySelector(".btn-search");

function updateButtonState() {
    if (inputSearch.value.trim() === "") {
        btnSearch.setAttribute("disabled", "");
    } else {
        btnSearch.removeAttribute("disabled");
    }
}

inputSearch.addEventListener("input", updateButtonState);
inputSearch.addEventListener("change", updateButtonState);
inputSearch.addEventListener("keyup", updateButtonState);

updateButtonState();

function performSearch() {
    const searchValue = inputSearch.value.trim();
    if (searchValue !== "") {
        location.href = 'evo://interface/navigate/' + encodeURIComponent(searchValue);
    }
}

inputSearch.addEventListener("keyup", (e) => {
    if (e.key === "Enter") {
        performSearch();
    }
});

btnSearch.addEventListener("click", () => {
    performSearch();
});