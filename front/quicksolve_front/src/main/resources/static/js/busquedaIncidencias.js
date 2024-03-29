
$(document).ready(function (){
    $("#search-input-incidences").on('input', function (evt){
        if (evt.currentTarget.value){
            $.ajax('/rest/incidencias/buscar', {
                method: 'POST',
                contentType: 'application/json',
                headers: {
                    "Authorization": localStorage.getItem("token")
                },
                data: evt.currentTarget.value,
                success: function (data) {
                    $("#resultados-incidencias").html("")
                    if (data.length !== 0){
                        data.forEach(i => {
                            $("#resultados-incidencias").append(`
                                <div class="card card-animation card mb-4 mx-2 incidence" style="width: 18rem;" onclick="window.location.href='/incidencia/${i.id}'">
                                    <div class="card-body">
                                        <h5 class="card-title text-truncate">${i.title}</h5>
                                        <p class="card-text text-truncate mb-1">${i.description}</p>
                                        <div class="d-flex justify-content-between">
                                            <small>${i.dateStart}</small>
                                        </div>
                                    </div>
                                </div>
                            `);
                        });
                    } else {
                        notFoundResults();
                    }
                }
            });
        } else {
            notFoundResults();
        }
    });
});

function notFoundResults(){
    $("#resultados-incidencias").html(`
       <div class="d-flex justify-content-center text-center flex-column">
            <img src="/img/person-dealing-error.svg" class="img-fluid mt-2" style="max-width: 385px;">
       </div>
    `);
}