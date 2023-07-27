function image(message) {
    const data = {};
    data["msg"] = message;
    $.ajax({
        url: "http://82.156.167.18:9298/image",
        type: "POST",
        data: JSON.stringify(data),
        success: function (response) {

            console.log("responis: ", response)
            const imageUrl = response.data[0].url;
            console.log("imageUrl is:", imageUrl)
            // 创建答案模板
            //       let template = `
            //   <div class="answer">
            //     <div class="icon left"><img src="images/ai.png"></div>
            //     <div class="answer_text">
            //       <div class="conbox">
            //         <div class="content_text"><img src="${imageUrl}" /></div>
            //         <div class="cz">
            //           <a href="javascript:;" class="zan">
            //             <em class="iconfont icon-zan"></em> 赞
            //           </a>
            //           <a href="javascript:;" class="cai">
            //             <em class="iconfont icon-cai"></em> 踩
            //           </a>
            //         </div>
            //       </div>
            //     </div>
            //   </div>
            // `;
            //
            //       fit_screen();
            const content = document.querySelector('.content');
            const img = document.createElement('img');
            img.src = imageUrl;

            // 将 img 元素添加到聊天框中
            content.appendChild(img);
        },

        error: function (http) {
            alert("每次队列只处理一个问题，请耐心等待10s左右重新提问");
        }
    });
}


function genimage() {
    $('.write_list').remove();
    var text = $("#textarea").val();
    if (text == '') {
        alert('请输入聊天内容！');
        $('.chat_box input').focus();
        $('body').css('background-image', 'url(images/bg.png)');
    } else {
        let html = ''
        let send_time = new Date();
        // let send_time_str = get_time_str(send_time);
        // let userId = localStorage.getItem("userId") == null ? "" : localStorage.getItem("userId");
        html += '<div class="item item-right"><div class="bubble bubble-right markdown">' + marked.marked(text) + '</div><div class="avatar"><img src="static/people.jpg" /></div></div>';
        $(".content").append(html);
        $("#textarea").val("");
        $(".content").scrollTop($(".content")[0].scrollHeight);

        image(text);
    }
}

function fit_screen() {
    $('.speak_box, .speak_window').animate({scrollTop: $('.speak_box').height()}, 500);
}

function auto_width() {
    $('.question_text').css('max-width', $('.question').width() - 60);
}
