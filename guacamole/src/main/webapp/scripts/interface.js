
var menu_shaded = false;

var shade_interval = null;
var show_interval = null;

function shadeMenu() {

    if (!menu_shaded) {

        var step = Math.floor(menu.offsetHeight / 5) + 1;
        var offset = 0;
        menu_shaded = true;

        window.clearInterval(show_interval);
        shade_interval = window.setInterval(function() {

            offset -= step;
            menu.style.top = offset + "px";

            if (offset <= -menu.offsetHeight) {
                window.clearInterval(shade_interval);
                menu.style.visiblity = "hidden";
            }

        }, 30);
    }

}

function showMenu() {

    if (menu_shaded) {

        var step = Math.floor(menu.offsetHeight / 5) + 1;
        var offset = -menu.offsetHeight;
        menu_shaded = false;
        menu.style.visiblity = "";

        window.clearInterval(shade_interval);
        show_interval = window.setInterval(function() {

            offset += step;

            if (offset >= 0) {
                offset = 0;
                window.clearInterval(show_interval);
            }

            menu.style.top = offset + "px";

        }, 30);
    }

}

