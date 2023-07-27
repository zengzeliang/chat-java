function image(message) {
    const data = {};
    data["msg"] = message;
    $.ajax({
        url: "http://82.156.167.18:9298/image",
        type: "POST",
        data: JSON.stringify(data),
        success: function (response) {
            console.log("responis: ", response)
            response = JSON.parse(response);
            const imageUrl = response.data[0].url;
            console.log("imageUrl is:", imageUrl)

            let div =  document.createElement('div');
            div.innerHTML = marked.marked("<img src = '" + imageUrl + "'/>");
            $(".content").scrollTop($(".content")[0].scrollHeight);

            $("#stop-btn").hide();

            getSelectedChatInfo().messages_total += 1;
            renderHeadInfo();
        },

        error: function (http) {
            alert("每次队列只处理一个问题，请耐心等待10s左右重新提问");
        }
    });
}


function genimage() {


}
