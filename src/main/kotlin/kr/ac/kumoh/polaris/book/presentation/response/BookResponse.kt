package kr.ac.kumoh.polaris.book.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.ac.kumoh.polaris.book.implement.dto.BookResult
import java.time.LocalDate

data class BookResponse(
    @Schema(description = "ISBN입니다. 13자리 길이를 가집니다.", example = "9791198363510", nullable = false)
    val isbn: String,
    @Schema(description = "도서명입니다.", example = "아몬드", nullable = false)
    val title: String,
    @Schema(description = "저자입니다.", example = "지은이: 손원평", nullable = false)
    val author: String?,
    @Schema(description = "출판사입니다.", example = "다즐링", nullable = false)
    val publisher: String?,
    @Schema(description = "도서 설명입니다.", example = "두 소년이 타인과 관계 맺고 성장하는 과정을 끝까지 섬세하게 짚어 나가는 작가의 문장은, 겉보기에 괴물로 보인다 할지라도 그 내면에는 언제나 괴물이 되지 않기 위한 눈물겨운 분투가 숨어 있다는 진실을 설득력 있게 보여 준다. 캐릭터의 매력, 그리고 깊은 성찰로 빚어낸 두 인물의 관계에 깃든 아름다움에서 이 작품이 문학적으로 의미 있는 성취를 이루었음을 알 수 있다.\\r\\n-제10회 창비청소년문학상 심사위원 권여선 김지은 오세란 정은숙\\r\\n\\r\\n다른 사람의 아픔에 공감하지 못한다는 것은 얼마나 불행한 일인가? 손원평 작가의 『아몬드』는 타인과 관계 맺고 슬픔에 공감하며 성장해 나가는 과정을 탁월하게 묘사한다. 몸이 자라는 만큼 마음도 함께 자라던 시절, 그 시간을 함께 보낸 주인공 ‘나’와 ‘곤’의 이야기. 그들이 만나 ‘친구’라는 이름이 붙기까지 보내 온 몇 해의 계절을 떠올리면, 책을 덮고 나서도 코끝에 처연하고 시린 기운이 전해지는 것만 같다.\\r\\n-이재용 감독(「두근두근 내 인생」 「스캔들」 연출)\\r\\n\\r\\n20년 넘게 영화 일을 하며 생긴 직업병 같은 게 있다. 두 시간을 넘는 콘텐츠에는 집중하기가 어렵다는 거다. 200페이지가 넘는 소설을 읽어야 하다니……. 그렇지만 『아몬드』는 끊임없이 궁금증과 흥미를 유발하여 마지막 페이지까지 금세 넘어갔다. 담담히 오늘을 살아가는 수많은 우리들에게 세상을 버틸 용기와 힘을 주는 소설이다.\\r\\n-장원석 PD(「최종병기 활」 「범죄도시」 제작)\\r\\n", nullable = false)
    val description: String?,
    @Schema(description = "출판일입니다.", example = "20230714", nullable = false)
    val publicationDate: LocalDate?,
    @Schema(description = "도서 표지 URL입니다.", example = "https://nl.go.kr/seoji/fu/ecip/dbfiles/CIP_FILES_TBL/2023/06/9791198363510.jpg", nullable = false)
    val coverImageUrl: String?
) {
    companion object {
        fun from(result: BookResult): BookResponse =
            BookResponse(
                isbn = result.isbn,
                title = result.title,
                author = result.author,
                publisher = result.publisher,
                description = result.description,
                publicationDate = result.publicationDate,
                coverImageUrl = result.coverImageUrl
            )
    }
}
