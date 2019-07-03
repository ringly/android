package com.ringly.ringly.bluetooth;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Created by Monica on 4/18/2017.
 */

public class DevicesNameRewriteUtils {


    public static String[]roseRenameList = {"006A","0242","03BD","048E","04B4","04B5","0514","0586","05B8","05BC","068E","0708","07D5",
                                    "0879","087E","0887","090B","0941","094B","09AA","09BC","09C9","0AD9","0C89","0D6F","0D7E",
                                    "0DCF","0E17","0E99","0EB8","10D9","10EF","10F1","1132","1159","121B","1261","133A","1372",
                                    "1446","1548","156A","15AE","15AF","15D6","15EF","1735","176A","1789","1789","17CF","1861",
                                    "18AE","19F0","1A45","1A86","1AC9","1AD3","1B65","1B68","1C53","1C75","1CE6","1D58","1EB1",
                                    "1ED9","1EFC","22B6","22E5","231D","2327","2442","24CF","25DA","26AE","2779","27AA","286C",
                                    "29EF","2A37","2C63","2CF2","2D18","2D2E","2D37","2FF8","3010","307F","3113","31B4","3281",
                                    "329F","33D1","340B","341C","345C","353A","36A3","38B9","3948","3985","3A6F","3BE4","3C86",
                                    "3C90","3CB7","3D69","3DE1","3F29","3FA2","3FB3","40B1","4114","4212","4265","42B5","438D",
                                    "4421","443F","4667","46B8","473B","4753","47B7","4800","4984","49BE","4A2F","4ADD","4BE3",
                                    "4BE7","4CD0","4D78","4E37","4FE4","507E","50A1","5105","5198","51A0","51C9","51D1","51D6",
                                    "52EE","52FC","5315","5614","567A","5728","573C","576E","578C","5879","592C","5938","5A26",
                                    "5A2B","5AC8","5B96","5C1C","5C35","5CBE","5D0F","5D41","5D49","5D68","5DC0","5DFE","5E80",
                                    "5EAE","5FCA","60C1","61CB","6329","6349","6391","63A7","6499","6572","65E7","6640","665E",
                                    "66F7","6780","67B9","687D","68DD","68E9","6974","6AA4","6AA9","6ACA","6B96","6CBB","6CDB",
                                    "6CFB","6DA8","6DE0","6E21","6ED6","6F6F","7009","7009","707D","70CF","7318","75A0","7651",
                                    "769F","76E4","7717","77ED","7864","7870","78AA","78FD","79F8","7A13","7A31","7A34","7A3B",
                                    "7A4F","7A74","7CA4","7CE1","7CFF","7D13","7D49","7D65","7E3F","7ED7","7F50","805F","81B0",
                                    "81D3","8419","84BA","8547","8568","858A","8638","8676","86AA","86E2","8776","87F9","888D",
                                    "8936","89EB","8A14","8A2D","8AA9","8B50","8B81","8C1F","8CA7","8E0A","8E60","8E7A","8F18",
                                    "90AE","9115","921C","9247","92B0","9368","9373","93D1","944D","94A1","9613","978E","9836",
                                    "9849","988B","98C8","98F7","9A72","9B44","9B9E","9C16","9C36","9CC1","9CC7","9CCD","9CD9",
                                    "9CF2","9D42","9DA9","9E9F","A048","A050","A0C9","A117","A241","A27F","A2CA","A2E8","A2FD",
                                    "A37E","A3BC","A3FF","A465","A497","A5B7","A5D2","A638","A656","A6C8","A6D1","A6FC","A8E3",
                                    "A926","A9A2","A9BC","AA28","AAA2","AB87","ABA9","AC49","AC64","AC65","ACDE","AD23","AD38",
                                    "AD5F","ADE1","AE0F","AF06","AF82","AFAC","B093","B0E3","B14E","B19E","B213","B24A","B25B",
                                    "B2A4","B316","B3A3","B4CF","B4E9","B50F","B53A","B599","B5D0","B5F8","B622","B6B5","B87C",
                                    "B8A4","B8AB","B95F","BA0A","BA70","BA85","BDBF","BDCC","BEBB","BF10","BFF7","C01E","C0DD",
                                    "C199","C1CF","C22A","C28A","C308","C332","C519","C548","C593","C65C","C78D","C8BF","C8CF",
                                    "C8E4","C912","C95D","CAEF","CB12","CC55","CC81","CD08","CD62","CDB2","CDEE","CE8D","CF44",
                                    "D0A0","D166","D1AD","D1BA","D210","D2F5","D332","D358","D366","D420","D477","D638","D64F",
                                    "D669","D684","D77C","D810","D82C","D87B","D8A9","D8EE","D906","D951","D9A3","D9AF","DA15",
                                    "DAF5","DB88","DB8B","DBD9","DC33","DC6D","DE2B","DE8C","DE97","E018","E0A9","E0B0","E0DF",
                                    "E0F0","E0FB","E242","E2D0","E2E3","E31E","E364","E37B","E554","E564","E5E8","E7B2","E7FB",
                                    "E856","E8C1","E957","E9A6","EC01","EC3F","ECFC","ED06","ED16","ED20","ED79","EE10","EECC",
                                    "EEE5","EEEB","F021","F024","F027","F058","F059","F0A1","F1D5","F33F","F3C2","F47C","F4AC",
                                    "F5FA","F664","F85B","F865","F931","F985","FA36","FA36","FAFE","FB12","FC83","FCA6","FCFC","FEFE","FFE4"};


    public static String[] go01RenameList = {"D487","614B","9B5F","3FF1","09A7","A3BB","A1B1","55C8","AAF6","64A2",
            "8D90","FEC1","600D","4E2C","41B5","53CC","947F","2A60","A21A","D0F0",
            "DCFE","D329","9E55","0BF0","B0A4","3781","F7E6","3CDA","5FE5","57F1",
            "1A47","F7E1","F69B","8911","18AE","FFE1","48CC","52C1","72DD","AA4E",
            "1A0A","3606","8F17","0FCD","7EE9","D16A","54EE","D179","34C5","ECCF",
            "B869","A58C","8DDB","8DE5","70D6","C485","19C6","6B5A","7A2A","B05C",
            "50C3","CD27","D08D","36D1","2F4F","FFC5","3A04","0F11","C3F2","DFD6",
            "D80B","B20D","1921","148E","AAC4","CAB1","B6F9","59A5","600B","2DF2",
            "7933","F544","2C9A","6D11","81DB","3A20","53EC","CD9B","469C","6E0E",
            "2D7F","221E","1EAC","364C","23B1","45D7","E8EF","4892","441A","B74F",
            "332D","26F1","1C46","4989","FF60","50C8","3518","0416","2CB0","CAB7",
            "2ABA","2F8D","FD14","22B5","181D","C080","F888","4C54","FD59","BA72",
            "56BC","A8D7","B121","41DA","376A","062B","82BD","ABA7","3615","5F80",
            "5185","AE92","64BB","333D","3E15","2196","3C54","2DC4","EA7F","817E",
            "268D","72E0","E0E3","A0B7","89BB","1985","6682","96EC","2D14","7425",
            "A982","B483","A602","34AB","543E","5A3E","FB26","90FF","F127","26DF",
            "1373","C1AD","7377","27C5","E194","F260","D6D6","0FEA","4F16","A00A",
            "1473","9736","8F5F","1173","C151","8E19","C503","AE5D","79A7","3952",
            "453D","5E32","1536","B293","7254","EF3F","583C","00F4","020F","5E2C",
            "F3EF","168C","77F8","CF37","B500","C759","F67A","CBFA","7760","06E9",
            "9D6C","891E","BF0E","AEA1","F02F","10BC","C7DA","8699","0260","FFF4",
            "24AC","FEAE","4AE0","7E0A","0837","3779","BE1C","ACCB","D160","F064",
            "843C","7590","8834","8074","6718","5720","4F54","6F41","4565","98DF",
            "22D0","8DF8","A72E","F28A","2C75","60AF","DD93","AB00","ACDB","7974",
            "BFF3","A865","9918","FD05","62E0","7958","312A","EED8","E572","E199",
            "6D12","D371","770E","54AA","7596","4B54","059E","F2C3","1E04","B5D9",
            "CB9F","7297","0706","7A3A","7BA8","92B8","7473","273D","AA82","A281",
            "D40B","4C7E","10F0","80CD","0235","E10C","509F","B5BA","0B8A"};


    public static String[] go02RenameList = {"B1A8","BBBB","CB89","7B7E","7276","9FFD","EDC7","FBB4","F057","E047",
            "D473","9A3B","5269","D17A","F83D","5CC1","AC69","9736","F902","F8B4",
            "55BA","2EE1","5A3C","E07D","84EC","916F","656A","38B1","BCA0","070B",
            "829E","1956","20AB","20CE","68BE","DEF0","DD6A","F817","7532","02CA",
            "A82B","9D77","B26F","EF43","172E","7116","ECAE","EEAF","B484","406E",
            "FE96","45EE","D6CA","B0B0","E15C","B91F","EFCA","C55D","1219","B6E6",
            "99CE","E2EF","04E4","2F4A","9233","9CE2","FC88","2BDD","FDE8","BB10",
            "A799","B41D","208A","B46B","E026","9664","4E5A","4B8A","948D","69CE",
            "E740","1FC1","3583","D2F3","CED4","1DB3","AA67","7AC5","E8AD","CF7A",
            "58C1","A0DE","17AC","A1F1","9F54","0AC0","5D28","5D45","99B4","D124",
            "1AD5","CE67","3E6F","693A","E30B","0F24","6F95","F9B1","E904","A411",
            "C160","4006","642C","60B9","2FFC","3D76","6862","B310","5C8E","07C1",
            "4E42","9964","BDFE","DC1D","44C5","072D","E2F3","A4B8","DF6F","F01D",
            "0D11","F8BD","D687","5E08","94E0","BC4E","2A29","3DDC","025D","4410",
            "E9C6","CAB9","A471","8B53","5AF2","66A6","8DBB","FE85","9FC5","383C",
            "8A18","5FE6","348A","EF28","7E4E","4004","1BD4","E0CC","B30A","2886",
            "C332","198C","5F29","352C","2D44","E900","1015","E366","331B","FCB7",
            "4F4B","912E","4ED4","4C19","0E7A","6069","E16C","8856","C36A","2B8A",
            "FE05","2D9F","E250","2ADF","384E","68AC","7D89","C240","F713","1652",
            "0851","8A5E","565F","7FD2","3ADF","A16A","D5BD","0F8B","147C","709B",
            "C087","A9E1","0DEA","C30E","1AC6","08B6","E0AA","5974","1CB0","D0AB",
            "3DD4","7E04","981F","BFF6","F10A","91C8","7619","D2A1","986E","B059",
            "E998","9150","A989","D4EA","393C","E51A","7A96","3A16","12A8","3F70",
            "5E5B","6EDE","40F9","CCCE","C6DB","3A69","661D","C624","1313"};

    public static boolean isInRenameList(String value, String[] addressRenameList) {
        return ArrayUtils.contains(addressRenameList, value);
    }

    /**
     * Return true if the given deviceName and deviceAddress should be rewritten from ROSE to DATE.
     * @param deviceName This is the complete name, ex: RINGLY - LOVE (a301)
     * @param deviceAddress This is the complete device address, ex: AA33:EC3A:5534:F0A1
     * @return
     */
    public static boolean needsRoseToDateRewrite(String deviceName, String deviceAddress) {
        return needsRewrite(deviceName, deviceAddress, "ROSE" , roseRenameList);
    }

    /**
     * Return true if the given deviceName and deviceAddress should be rewritten from LOVE to GO01.
     * @param deviceName This is the complete name, ex: RINGLY - LOVE (a301)
     * @param deviceAddress This is the complete device address, ex: AA33:EC3A:5534:F0A1
     * @return
     */
    public static boolean needsLoveToGO01Rewrite(String deviceName, String deviceAddress) {
        return needsRewrite(deviceName, deviceAddress, "LOVE" , go01RenameList);
    }

    /**
     * Return true if the given deviceName and deviceAddress should be rewritten from LOVE to GO02.
     * @param deviceName This is the complete name, ex: RINGLY - LOVE (a301)
     * @param deviceAddress This is the complete device address, ex: AA33:EC3A:5534:F0A1
     * @return
     */
    public static boolean needsLoveToGO02Rewrite(String deviceName, String deviceAddress) {
        return needsRewrite(deviceName, deviceAddress, "LOVE" , go02RenameList);
    }
    /**
     *
     * @param deviceName
     * @param deviceAddress
     * @param advertisingName
     * @param addressRenameList
     * @return
     */
    private static boolean needsRewrite(String deviceName, String deviceAddress, String advertisingName, String[]addressRenameList) {
        deviceAddress = deviceAddress.replaceAll(":","");
        deviceAddress = deviceAddress.length() <= 4
                ? deviceAddress : deviceAddress.substring(deviceAddress.length() - 4, deviceAddress.length());
        deviceAddress = deviceAddress.toUpperCase();
        return RingName.isEqualToAdvertisingName(deviceName, advertisingName) && DevicesNameRewriteUtils.isInRenameList(deviceAddress, addressRenameList);

    }
}
